package com.example.endtrem.service;

import com.example.endtrem.dto.AggregatedProfileDTO;
import com.example.endtrem.dto.AnalysisRequest;
import com.example.endtrem.dto.AnalysisResponse;
import com.example.endtrem.model.AnalysisResult;
import com.example.endtrem.model.RepoSummary;
import com.example.endtrem.model.Repository;
import com.example.endtrem.model.User;
import com.example.endtrem.repository.AnalysisResultRepository;
import com.example.endtrem.repository.RepoSummaryRepository;
import com.example.endtrem.repository.RepositoryRepository;
import com.example.endtrem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {
    
    private final RepositoryRepository repositoryRepository;
    private final RepoSummaryRepository repoSummaryRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final UserRepository userRepository;
    private final GitHubService gitHubService;
    private final AIService aiService;
    
    /**
     * Main analysis pipeline - OPTIMIZED VERSION
     * 1. Fetch repositories (if needed)
     * 2. Filter based on user selection
     * 3. Extract skills locally from READMEs (no AI call)
     * 4. Aggregate summaries
     * 5. Single AI call for final analysis
     * 6. Store and return results
     */
    public AnalysisResponse runAnalysis(String userId, AnalysisRequest request) {
        log.info("Starting analysis for user: {} with filter mode: {}", userId, request.getFilterMode());
        
        // Determine filter mode (default to TIME_BASED for backward compatibility)
        AnalysisRequest.FilterMode filterMode = request.getFilterMode() != null 
            ? request.getFilterMode() 
            : AnalysisRequest.FilterMode.TIME_BASED;
        
        List<Repository> repos;
        
        switch (filterMode) {
            case SELECTED_REPOS:
                // Only analyze selected repos - skip GitHub fetch
                if (request.getSelectedRepoIds() == null || request.getSelectedRepoIds().isEmpty()) {
                    throw new RuntimeException("No repositories selected for analysis.");
                }
                repos = repositoryRepository.findAllById(request.getSelectedRepoIds())
                    .stream()
                    .filter(r -> r.getUserId().equals(userId))
                    .collect(Collectors.toList());
                break;
                
            case COMBINED:
                // Fetch by time, then filter to selected
                gitHubService.fetchAndSaveRepositories(userId, request.getMonthsToAnalyze(), request.isIncludeForkedRepos());
                if (request.getSelectedRepoIds() != null && !request.getSelectedRepoIds().isEmpty()) {
                    Set<String> selectedSet = new HashSet<>(request.getSelectedRepoIds());
                    repos = repositoryRepository.findByUserId(userId).stream()
                        .filter(r -> selectedSet.contains(r.getId()))
                        .collect(Collectors.toList());
                } else {
                    repos = repositoryRepository.findByUserId(userId);
                }
                break;
                
            case TIME_BASED:
            default:
                // Original behavior - fetch and get unprocessed
                gitHubService.fetchAndSaveRepositories(userId, request.getMonthsToAnalyze(), request.isIncludeForkedRepos());
                repos = repositoryRepository.findByUserIdAndProcessed(userId, false);
                break;
        }
        
        // Filter out excluded repos
        if (request.getExcludeRepos() != null && !request.getExcludeRepos().isEmpty()) {
            Set<String> excludeSet = new HashSet<>(request.getExcludeRepos());
            repos = repos.stream()
                .filter(r -> !excludeSet.contains(r.getName()))
                .collect(Collectors.toList());
        }
        
        if (repos.isEmpty()) {
            throw new RuntimeException("No repositories found to analyze. Try selecting repositories or increasing the time range.");
        }
        
        log.info("Processing {} repositories with optimized pipeline", repos.size());
        long startTime = System.currentTimeMillis();
        
        // Step 3: PIPELINE STAGE 1 - Extract skills locally from READMEs (NO AI CALL - just parsing)
        // This is the "small pipeline" that summarizes each README into languages/skills
        List<RepoSummary> summaries = new ArrayList<>();
        List<AnalysisResult.RepoExtraction> repoExtractions = new ArrayList<>();
        
        for (Repository repo : repos) {
            // Use local extraction instead of AI call - this is the small pipeline
            RepoSummary summary = extractSkillsLocally(repo);
            summary.setRepositoryId(repo.getId());
            summary.setUserId(userId);
            
            // Track extraction for pipeline summary
            repoExtractions.add(AnalysisResult.RepoExtraction.builder()
                .repoId(repo.getId())
                .repoName(repo.getName())
                .languages(summary.getTechnologies())
                .frameworks(summary.getFrameworks())
                .tools(new ArrayList<>())
                .skills(summary.getKeywords())
                .complexity(summary.getComplexity().name().toLowerCase())
                .build());
            
            // Save summary
            repoSummaryRepository.save(summary);
            summaries.add(summary);
            
            // Mark repo as processed
            repo.setProcessed(true);
            repositoryRepository.save(repo);
        }
        
        // Step 4: PIPELINE STAGE 2 - Aggregate all summaries into a single profile
        // This combines the small pipelines into the main pipeline
        AggregatedProfileDTO aggregatedProfile = aggregateSummaries(userId, summaries, repos);
        
        // Step 5: PIPELINE STAGE 3 - Try AI analysis, fallback to local analysis if rate limited
        // The main pipeline where AI knows what user knows and doesn't
        AnalysisResult result;
        try {
            result = aiService.analyzeProfile(aggregatedProfile);
        } catch (Exception e) {
            log.warn("AI analysis failed ({}), using local analysis fallback", e.getMessage());
            result = generateLocalAnalysis(aggregatedProfile);
        }
        
        // Add pipeline summary to result
        long processingTime = System.currentTimeMillis() - startTime;
        AnalysisResult.PipelineSummary pipelineSummary = AnalysisResult.PipelineSummary.builder()
            .totalLanguages((int) aggregatedProfile.getLanguages().size())
            .totalFrameworks((int) aggregatedProfile.getFrameworks().size())
            .totalTools((int) aggregatedProfile.getTools().size())
            .processingTimeMs(processingTime)
            .repoExtractions(repoExtractions)
            .build();
        result.setPipelineSummary(pipelineSummary);
        
        result = analysisResultRepository.save(result);
        
        // Update user's last analysis timestamp
        User user = userRepository.findById(userId).orElseThrow();
        user.setLastAnalysisAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Analysis completed for user: {}", userId);
        
        return mapToResponse(result);
    }
    
    /**
     * Extract skills and technologies locally from README without AI call
     * This significantly reduces token usage and API calls
     */
    private RepoSummary extractSkillsLocally(Repository repo) {
        Set<String> technologies = new HashSet<>();
        Set<String> frameworks = new HashSet<>();
        Set<String> features = new HashSet<>();
        String projectType = "Unknown";
        RepoSummary.ComplexityLevel complexity = RepoSummary.ComplexityLevel.INTERMEDIATE;
        
        // Add primary language
        if (repo.getLanguage() != null) {
            technologies.add(repo.getLanguage());
        }
        
        // Add topics as technologies/keywords
        if (repo.getTopics() != null) {
            technologies.addAll(repo.getTopics());
        }
        
        String readme = repo.getReadmeContent();
        String description = repo.getDescription();
        String combined = ((readme != null ? readme : "") + " " + (description != null ? description : "")).toLowerCase();
        
        // Detect frameworks
        Map<String, String> frameworkPatterns = Map.ofEntries(
            Map.entry("spring boot", "Spring Boot"),
            Map.entry("springboot", "Spring Boot"),
            Map.entry("spring-boot", "Spring Boot"),
            Map.entry("spring framework", "Spring"),
            Map.entry("react", "React"),
            Map.entry("next.js", "Next.js"),
            Map.entry("nextjs", "Next.js"),
            Map.entry("vue", "Vue.js"),
            Map.entry("angular", "Angular"),
            Map.entry("express", "Express.js"),
            Map.entry("django", "Django"),
            Map.entry("flask", "Flask"),
            Map.entry("fastapi", "FastAPI"),
            Map.entry("laravel", "Laravel"),
            Map.entry("rails", "Ruby on Rails"),
            Map.entry("ruby on rails", "Ruby on Rails"),
            Map.entry("nest.js", "NestJS"),
            Map.entry("nestjs", "NestJS"),
            Map.entry("svelte", "Svelte"),
            Map.entry("tailwind", "TailwindCSS"),
            Map.entry("bootstrap", "Bootstrap"),
            Map.entry("material-ui", "Material-UI"),
            Map.entry("hibernate", "Hibernate"),
            Map.entry("junit", "JUnit"),
            Map.entry("pytest", "pytest"),
            Map.entry("jest", "Jest")
        );
        
        for (Map.Entry<String, String> entry : frameworkPatterns.entrySet()) {
            if (combined.contains(entry.getKey())) {
                frameworks.add(entry.getValue());
            }
        }
        
        // Detect technologies
        Map<String, String> techPatterns = Map.ofEntries(
            Map.entry("mongodb", "MongoDB"),
            Map.entry("postgresql", "PostgreSQL"),
            Map.entry("postgres", "PostgreSQL"),
            Map.entry("mysql", "MySQL"),
            Map.entry("redis", "Redis"),
            Map.entry("elasticsearch", "Elasticsearch"),
            Map.entry("docker", "Docker"),
            Map.entry("kubernetes", "Kubernetes"),
            Map.entry("k8s", "Kubernetes"),
            Map.entry("aws", "AWS"),
            Map.entry("azure", "Azure"),
            Map.entry("gcp", "Google Cloud"),
            Map.entry("firebase", "Firebase"),
            Map.entry("graphql", "GraphQL"),
            Map.entry("rest api", "REST API"),
            Map.entry("restful", "REST API"),
            Map.entry("websocket", "WebSocket"),
            Map.entry("jwt", "JWT"),
            Map.entry("oauth", "OAuth"),
            Map.entry("git", "Git"),
            Map.entry("github actions", "GitHub Actions"),
            Map.entry("ci/cd", "CI/CD"),
            Map.entry("webpack", "Webpack"),
            Map.entry("vite", "Vite"),
            Map.entry("npm", "npm"),
            Map.entry("maven", "Maven"),
            Map.entry("gradle", "Gradle")
        );
        
        for (Map.Entry<String, String> entry : techPatterns.entrySet()) {
            if (combined.contains(entry.getKey())) {
                technologies.add(entry.getValue());
            }
        }
        
        // Detect features
        Map<String, String> featurePatterns = Map.ofEntries(
            Map.entry("authentication", "Authentication"),
            Map.entry("authorization", "Authorization"),
            Map.entry("database", "Database Integration"),
            Map.entry("api", "API Development"),
            Map.entry("test", "Testing"),
            Map.entry("unit test", "Unit Testing"),
            Map.entry("integration test", "Integration Testing"),
            Map.entry("deploy", "Deployment"),
            Map.entry("responsive", "Responsive Design"),
            Map.entry("machine learning", "Machine Learning"),
            Map.entry("ml", "Machine Learning"),
            Map.entry("ai", "AI/ML"),
            Map.entry("data analysis", "Data Analysis"),
            Map.entry("realtime", "Real-time"),
            Map.entry("real-time", "Real-time"),
            Map.entry("microservice", "Microservices"),
            Map.entry("caching", "Caching"),
            Map.entry("logging", "Logging"),
            Map.entry("monitoring", "Monitoring")
        );
        
        for (Map.Entry<String, String> entry : featurePatterns.entrySet()) {
            if (combined.contains(entry.getKey())) {
                features.add(entry.getValue());
            }
        }
        
        // Detect project type
        if (combined.contains("api") || combined.contains("rest") || combined.contains("backend")) {
            projectType = "REST API";
        } else if (combined.contains("web app") || combined.contains("webapp") || combined.contains("frontend")) {
            projectType = "Web Application";
        } else if (combined.contains("cli") || combined.contains("command line")) {
            projectType = "CLI Tool";
        } else if (combined.contains("library") || combined.contains("package") || combined.contains("npm")) {
            projectType = "Library";
        } else if (combined.contains("mobile") || combined.contains("android") || combined.contains("ios")) {
            projectType = "Mobile App";
        } else if (combined.contains("game")) {
            projectType = "Game";
        } else if (combined.contains("bot") || combined.contains("discord") || combined.contains("telegram")) {
            projectType = "Bot";
        }
        
        // Estimate complexity based on indicators
        int complexityScore = 0;
        if (frameworks.size() >= 3) complexityScore += 2;
        if (technologies.size() >= 5) complexityScore += 2;
        if (features.contains("Microservices")) complexityScore += 2;
        if (features.contains("Testing")) complexityScore += 1;
        if (combined.contains("docker") && combined.contains("kubernetes")) complexityScore += 2;
        if (repo.getStargazersCount() > 10) complexityScore += 1;
        if (repo.getSize() > 5000) complexityScore += 1; // Large repo
        
        if (complexityScore >= 5) {
            complexity = RepoSummary.ComplexityLevel.ADVANCED;
        } else if (complexityScore >= 2) {
            complexity = RepoSummary.ComplexityLevel.INTERMEDIATE;
        } else {
            complexity = RepoSummary.ComplexityLevel.BEGINNER;
        }
        
        return RepoSummary.builder()
            .repoName(repo.getName())
            .technologies(new ArrayList<>(technologies))
            .frameworks(new ArrayList<>(frameworks))
            .features(new ArrayList<>(features))
            .projectType(projectType)
            .complexity(complexity)
            .description(repo.getDescription() != null ? repo.getDescription() : "")
            .keywords(repo.getTopics() != null ? new ArrayList<>(repo.getTopics()) : new ArrayList<>())
            .build();
    }
    
    /**
     * Aggregate all repo summaries into a single profile for efficient AI processing
     */
    private AggregatedProfileDTO aggregateSummaries(String userId, List<RepoSummary> summaries, List<Repository> repos) {
        Set<String> languages = new HashSet<>();
        Set<String> frameworks = new HashSet<>();
        Set<String> tools = new HashSet<>();
        Set<String> projectTypes = new HashSet<>();
        Set<String> features = new HashSet<>();
        Map<String, Integer> languageCount = new HashMap<>();
        Map<String, Integer> complexityCount = new HashMap<>();
        int totalStars = 0;
        
        // Aggregate from repos
        for (Repository repo : repos) {
            if (repo.getLanguage() != null) {
                languages.add(repo.getLanguage());
                languageCount.merge(repo.getLanguage(), 1, Integer::sum);
            }
            totalStars += repo.getStargazersCount();
        }
        
        // Aggregate from summaries
        for (RepoSummary summary : summaries) {
            if (summary.getTechnologies() != null) {
                tools.addAll(summary.getTechnologies());
            }
            if (summary.getFrameworks() != null) {
                frameworks.addAll(summary.getFrameworks());
            }
            if (summary.getFeatures() != null) {
                features.addAll(summary.getFeatures());
            }
            if (summary.getProjectType() != null) {
                projectTypes.add(summary.getProjectType());
            }
            if (summary.getComplexity() != null) {
                complexityCount.merge(summary.getComplexity().name(), 1, Integer::sum);
            }
        }
        
        // Detect potential gaps based on what's missing
        List<String> potentialGaps = detectGaps(languages, frameworks, features);
        
        return AggregatedProfileDTO.builder()
            .userId(userId)
            .languages(new ArrayList<>(languages))
            .frameworks(new ArrayList<>(frameworks))
            .tools(new ArrayList<>(tools))
            .projectTypes(new ArrayList<>(projectTypes))
            .features(new ArrayList<>(features))
            .languageDistribution(languageCount)
            .complexityDistribution(complexityCount)
            .totalProjects(repos.size())
            .totalStars(totalStars)
            .potentialGaps(potentialGaps)
            .build();
    }
    
    /**
     * Detect common skill gaps based on what technologies are present
     */
    private List<String> detectGaps(Set<String> languages, Set<String> frameworks, Set<String> features) {
        List<String> gaps = new ArrayList<>();
        
        // Common gap patterns
        boolean hasBackend = frameworks.stream().anyMatch(f -> 
            f.toLowerCase().contains("spring") || f.toLowerCase().contains("express") || 
            f.toLowerCase().contains("django") || f.toLowerCase().contains("flask"));
        boolean hasFrontend = frameworks.stream().anyMatch(f ->
            f.toLowerCase().contains("react") || f.toLowerCase().contains("vue") ||
            f.toLowerCase().contains("angular") || f.toLowerCase().contains("next"));
        boolean hasDatabase = features.stream().anyMatch(f ->
            f.toLowerCase().contains("database") || f.toLowerCase().contains("mongodb") ||
            f.toLowerCase().contains("sql") || f.toLowerCase().contains("orm"));
        boolean hasTesting = features.stream().anyMatch(f ->
            f.toLowerCase().contains("test") || f.toLowerCase().contains("junit") ||
            f.toLowerCase().contains("jest"));
        boolean hasCICD = features.stream().anyMatch(f ->
            f.toLowerCase().contains("ci") || f.toLowerCase().contains("cd") ||
            f.toLowerCase().contains("pipeline") || f.toLowerCase().contains("github actions"));
        boolean hasDocker = features.stream().anyMatch(f ->
            f.toLowerCase().contains("docker") || f.toLowerCase().contains("container"));
        
        if (!hasTesting) gaps.add("Testing/TDD");
        if (!hasCICD) gaps.add("CI/CD Pipelines");
        if (!hasDocker) gaps.add("Containerization/Docker");
        if (hasBackend && !hasFrontend) gaps.add("Frontend Development");
        if (hasFrontend && !hasBackend) gaps.add("Backend Development");
        if (!hasDatabase) gaps.add("Database Design");
        
        return gaps;
    }
    
    /**
     * Generate analysis locally when AI is unavailable (rate limited, etc.)
     * This provides a basic but useful analysis without any API calls
     */
    private AnalysisResult generateLocalAnalysis(AggregatedProfileDTO profile) {
        log.info("Generating local analysis fallback for user: {}", profile.getUserId());
        
        // Build skills from profile data
        List<AnalysisResult.Skill> strongSkills = new ArrayList<>();
        List<AnalysisResult.Skill> moderateSkills = new ArrayList<>();
        List<AnalysisResult.Skill> weakSkills = new ArrayList<>();
        
        // Languages as skills - use frequency to determine proficiency
        Map<String, Integer> langDist = profile.getLanguageDistribution();
        int maxLangCount = langDist.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        
        for (String lang : profile.getLanguages()) {
            int count = langDist.getOrDefault(lang, 1);
            int score = Math.min(95, 40 + (count * 50 / maxLangCount));
            
            AnalysisResult.Skill skill = AnalysisResult.Skill.builder()
                .name(lang)
                .category("language")
                .proficiencyScore(score)
                .evidence("Found in " + count + " project(s)")
                .projectCount(count)
                .trend(count >= 3 ? "improving" : "stable")
                .build();
            
            if (score >= 70) {
                strongSkills.add(skill);
            } else if (score >= 50) {
                moderateSkills.add(skill);
            } else {
                weakSkills.add(skill);
            }
        }
        
        // Frameworks as skills
        for (String framework : profile.getFrameworks()) {
            AnalysisResult.Skill skill = AnalysisResult.Skill.builder()
                .name(framework)
                .category("framework")
                .proficiencyScore(65)
                .evidence("Detected in project analysis")
                .projectCount(1)
                .trend("stable")
                .build();
            moderateSkills.add(skill);
        }
        
        // Tools as skills
        for (String tool : profile.getTools()) {
            AnalysisResult.Skill skill = AnalysisResult.Skill.builder()
                .name(tool)
                .category("tool")
                .proficiencyScore(55)
                .evidence("Used in projects")
                .projectCount(1)
                .trend("stable")
                .build();
            moderateSkills.add(skill);
        }
        
        int totalSkills = strongSkills.size() + moderateSkills.size() + weakSkills.size();
        
        AnalysisResult.SkillAnalysis skillAnalysis = AnalysisResult.SkillAnalysis.builder()
            .strongSkills(strongSkills)
            .moderateSkills(moderateSkills)
            .weakSkills(weakSkills)
            .missingSkills(profile.getPotentialGaps() != null ? profile.getPotentialGaps() : new ArrayList<>())
            .totalSkillsCount(totalSkills)
            .build();
        
        // Generate ENHANCED recommendations with three categories
        List<AnalysisResult.LearningRecommendation> skillsToLearn = new ArrayList<>();
        List<AnalysisResult.LearningRecommendation> skillsToImprove = new ArrayList<>();
        List<AnalysisResult.LearningRecommendation> practiceMore = new ArrayList<>();
        
        // Skills to LEARN (from gaps)
        if (profile.getPotentialGaps() != null) {
            int priority = 1;
            for (String gap : profile.getPotentialGaps()) {
                skillsToLearn.add(AnalysisResult.LearningRecommendation.builder()
                    .skill(gap)
                    .reason("This skill was not detected in your projects and would complement your existing expertise.")
                    .priority(Math.min(priority++, 5))
                    .resources(getDefaultResourcesForSkill(gap))
                    .estimatedTimeToLearn("2-4 weeks")
                    .category("learn_new")
                    .relatedSkills(new ArrayList<>(profile.getLanguages().subList(0, Math.min(2, profile.getLanguages().size()))))
                    .difficultyLevel("intermediate")
                    .careerImpact("high")
                    .build());
            }
        }
        
        // Skills to IMPROVE (moderate skills)
        int improveCount = 0;
        for (AnalysisResult.Skill skill : moderateSkills) {
            if (improveCount >= 3) break;
            skillsToImprove.add(AnalysisResult.LearningRecommendation.builder()
                .skill(skill.getName())
                .reason("You have experience with " + skill.getName() + " but there's room to deepen your knowledge.")
                .priority(improveCount + 1)
                .resources(List.of("Advanced " + skill.getName() + " tutorials", skill.getName() + " best practices", "Real-world " + skill.getName() + " projects"))
                .estimatedTimeToLearn("1-2 weeks")
                .category("improve_existing")
                .relatedSkills(new ArrayList<>())
                .difficultyLevel("intermediate")
                .careerImpact("medium")
                .build());
            improveCount++;
        }
        
        // Skills to PRACTICE MORE (strong skills)
        int practiceCount = 0;
        for (AnalysisResult.Skill skill : strongSkills) {
            if (practiceCount >= 2) break;
            practiceMore.add(AnalysisResult.LearningRecommendation.builder()
                .skill(skill.getName())
                .reason("You're proficient in " + skill.getName() + ". Consider contributing to open source or building complex projects to solidify expertise.")
                .priority(practiceCount + 1)
                .resources(List.of("Open source " + skill.getName() + " projects", skill.getName() + " coding challenges", "Build a production-grade " + skill.getName() + " app"))
                .estimatedTimeToLearn("Ongoing")
                .category("practice_more")
                .relatedSkills(new ArrayList<>())
                .difficultyLevel("advanced")
                .careerImpact("medium")
                .build());
            practiceCount++;
        }
        
        // Combined recommendations for backward compatibility
        List<AnalysisResult.LearningRecommendation> allRecommendations = new ArrayList<>();
        allRecommendations.addAll(skillsToLearn);
        allRecommendations.addAll(skillsToImprove);
        allRecommendations.addAll(practiceMore);
        
        // Enhanced recommendations object
        AnalysisResult.EnhancedRecommendations enhancedRecommendations = AnalysisResult.EnhancedRecommendations.builder()
            .skillsToLearn(skillsToLearn)
            .skillsToImprove(skillsToImprove)
            .practiceMore(practiceMore)
            .careerAdvice(generateCareerAdvice(profile))
            .nextMilestone(generateNextMilestone(profile))
            .build();
        
        // Determine experience level
        String experienceLevel = "Junior";
        Map<String, Integer> complexityDist = profile.getComplexityDistribution();
        int advanced = complexityDist.getOrDefault("ADVANCED", 0);
        int intermediate = complexityDist.getOrDefault("INTERMEDIATE", 0);
        
        if (advanced >= 3 || (advanced >= 1 && intermediate >= 3)) {
            experienceLevel = "Senior";
        } else if (advanced >= 1 || intermediate >= 2) {
            experienceLevel = "Mid";
        }
        
        // Determine specialization
        String specialization = "fullstack";
        boolean hasBackend = profile.getFrameworks().stream().anyMatch(f ->
            f.toLowerCase().contains("spring") || f.toLowerCase().contains("express") ||
            f.toLowerCase().contains("django") || f.toLowerCase().contains("flask"));
        boolean hasFrontend = profile.getFrameworks().stream().anyMatch(f ->
            f.toLowerCase().contains("react") || f.toLowerCase().contains("vue") ||
            f.toLowerCase().contains("angular") || f.toLowerCase().contains("next"));
        
        if (hasBackend && !hasFrontend) specialization = "backend";
        else if (hasFrontend && !hasBackend) specialization = "frontend";
        
        Map<String, Integer> skillDistribution = new HashMap<>();
        skillDistribution.put("languages", profile.getLanguages().size() * 10);
        skillDistribution.put("frameworks", profile.getFrameworks().size() * 15);
        skillDistribution.put("tools", profile.getTools().size() * 10);
        skillDistribution.put("concepts", profile.getFeatures().size() * 8);
        
        // Determine strengths and growth areas
        List<String> strengths = new ArrayList<>();
        List<String> areasForGrowth = new ArrayList<>();
        
        if (!strongSkills.isEmpty()) {
            strengths.add("Strong proficiency in " + strongSkills.get(0).getName());
        }
        if (hasBackend && hasFrontend) {
            strengths.add("Full-stack development capabilities");
        }
        if (profile.getPotentialGaps() != null && !profile.getPotentialGaps().isEmpty()) {
            areasForGrowth.addAll(profile.getPotentialGaps());
        }
        
        String careerStage = "junior";
        if (experienceLevel.equals("Senior")) careerStage = "senior";
        else if (experienceLevel.equals("Mid")) careerStage = "mid";
        
        AnalysisResult.DeveloperProfile developerProfile = AnalysisResult.DeveloperProfile.builder()
            .experienceLevel(experienceLevel)
            .primaryLanguages(new ArrayList<>(profile.getLanguages()))
            .primaryFrameworks(new ArrayList<>(profile.getFrameworks()))
            .projectTypes(profile.getProjectTypes())
            .specialization(specialization)
            .skillDistribution(skillDistribution)
            .strengths(strengths)
            .areasForGrowth(areasForGrowth)
            .careerStage(careerStage)
            .build();
        
        return AnalysisResult.builder()
            .userId(profile.getUserId())
            .skillAnalysis(skillAnalysis)
            .recommendations(allRecommendations)
            .enhancedRecommendations(enhancedRecommendations)
            .developerProfile(developerProfile)
            .totalRepositoriesAnalyzed(profile.getTotalProjects())
            .build();
    }
    
    private List<String> generateCareerAdvice(AggregatedProfileDTO profile) {
        List<String> advice = new ArrayList<>();
        
        if (profile.getLanguages().size() >= 3) {
            advice.add("You're versatile with multiple languages. Consider deepening expertise in one to become a specialist.");
        }
        if (profile.getFrameworks().isEmpty()) {
            advice.add("Learning a popular framework would accelerate your development speed and marketability.");
        }
        if (profile.getPotentialGaps() != null && profile.getPotentialGaps().contains("Testing/TDD")) {
            advice.add("Adding testing skills will make you more valuable to any team and improve code quality.");
        }
        if (profile.getTotalProjects() < 5) {
            advice.add("Building more projects will strengthen your portfolio and provide practical experience.");
        } else if (profile.getTotalProjects() >= 10) {
            advice.add("Great project portfolio! Consider contributing to open source to expand your network.");
        }
        
        if (advice.isEmpty()) {
            advice.add("Keep building projects and learning new technologies to advance your career.");
        }
        
        return advice;
    }
    
    private String generateNextMilestone(AggregatedProfileDTO profile) {
        Map<String, Integer> complexityDist = profile.getComplexityDistribution();
        int advanced = complexityDist.getOrDefault("ADVANCED", 0);
        int intermediate = complexityDist.getOrDefault("INTERMEDIATE", 0);
        
        if (advanced == 0) {
            return "Build an advanced project with complex architecture (microservices, distributed systems)";
        } else if (intermediate < 3) {
            return "Expand your portfolio with more intermediate-level projects";
        } else if (profile.getPotentialGaps() != null && !profile.getPotentialGaps().isEmpty()) {
            return "Learn " + profile.getPotentialGaps().get(0) + " to round out your skill set";
        }
        return "Consider mentoring others or contributing to major open source projects";
    }
    
    private List<String> getDefaultResourcesForSkill(String skill) {
        Map<String, List<String>> resourceMap = Map.of(
            "Testing/TDD", List.of("JUnit Documentation", "Testing Spring Boot Applications", "TDD by Example book"),
            "CI/CD Pipelines", List.of("GitHub Actions Documentation", "Jenkins Tutorial", "GitLab CI/CD Guide"),
            "Containerization/Docker", List.of("Docker Official Tutorial", "Docker for Developers", "Kubernetes Basics"),
            "Frontend Development", List.of("React Documentation", "Vue.js Guide", "Frontend Masters courses"),
            "Backend Development", List.of("Spring Boot Guide", "Node.js Documentation", "Backend Architecture Patterns"),
            "Database Design", List.of("SQL Tutorial", "MongoDB University", "Database Design Fundamentals")
        );
        return resourceMap.getOrDefault(skill, List.of("Online tutorials", "Official documentation", "Practice projects"));
    }
    
    public AnalysisResponse getLatestAnalysis(String userId) {
        return analysisResultRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
            .map(this::mapToResponse)
            .orElse(null);
    }
    
    public List<AnalysisResponse> getAnalysisHistory(String userId) {
        return analysisResultRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    public AnalysisResponse getAnalysisById(String analysisId) {
        return analysisResultRepository.findById(analysisId)
            .map(this::mapToResponse)
            .orElseThrow(() -> new RuntimeException("Analysis not found"));
    }
    
    private AnalysisResponse mapToResponse(AnalysisResult result) {
        return AnalysisResponse.builder()
            .analysisId(result.getId())
            .skillAnalysis(result.getSkillAnalysis())
            .recommendations(result.getRecommendations())
            .enhancedRecommendations(result.getEnhancedRecommendations())
            .developerProfile(result.getDeveloperProfile())
            .pipelineSummary(result.getPipelineSummary())
            .repositoriesAnalyzed(result.getTotalRepositoriesAnalyzed())
            .analyzedAt(result.getCreatedAt())
            .build();
    }
}
