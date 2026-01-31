package com.example.endtrem.service;

import com.example.endtrem.dto.AggregatedProfileDTO;
import com.example.endtrem.dto.AnalysisRequest;
import com.example.endtrem.dto.AnalysisResponse;
import com.example.endtrem.dto.ParsedReadmeDTO;
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
    private final ReadmeParserService readmeParserService;
    
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
        
        // Step 3: PIPELINE STAGE 1 - Parse READMEs locally (NO AI CALL)
        // This extracts tech stack, project maturity, engineering discipline, and vibe coding signals
        List<ParsedReadmeDTO> parsedReadmes = new ArrayList<>();
        List<RepoSummary> summaries = new ArrayList<>();
        List<AnalysisResult.RepoExtraction> repoExtractions = new ArrayList<>();
        
        for (Repository repo : repos) {
            // Use the new non-AI README parser
            ParsedReadmeDTO parsed = readmeParserService.parseReadme(repo);
            parsedReadmes.add(parsed);
            
            // Convert to RepoSummary for storage
            RepoSummary summary = convertToRepoSummary(parsed, repo, userId);
            repoSummaryRepository.save(summary);
            summaries.add(summary);
            
            // Track extraction for pipeline summary
            List<String> allTech = new ArrayList<>();
            allTech.addAll(parsed.getTechStack().getLanguages());
            allTech.addAll(parsed.getTechStack().getFrameworks());
            
            repoExtractions.add(AnalysisResult.RepoExtraction.builder()
                .repoId(repo.getId())
                .repoName(repo.getName())
                .languages(parsed.getTechStack().getLanguages())
                .frameworks(parsed.getTechStack().getFrameworks())
                .tools(parsed.getTechStack().getTools())
                .skills(parsed.getProjectMaturity().getFeaturesImplemented())
                .complexity(parsed.getComplexity().name().toLowerCase())
                .build());
            
            // Mark repo as processed
            repo.setProcessed(true);
            repositoryRepository.save(repo);
        }
        
        // Step 4: PIPELINE STAGE 2 - Aggregate all parsed data into comprehensive profile
        AggregatedProfileDTO aggregatedProfile = aggregateParsedReadmes(userId, parsedReadmes, repos);
        
        // Step 5: PIPELINE STAGE 3 - Single AI call with all pre-parsed data
        // AI now has complete context about what user knows, doesn't know, and their coding style
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
     * Convert ParsedReadmeDTO to RepoSummary for storage
     */
    private RepoSummary convertToRepoSummary(ParsedReadmeDTO parsed, Repository repo, String userId) {
        List<String> allTech = new ArrayList<>();
        allTech.addAll(parsed.getTechStack().getLanguages());
        allTech.addAll(parsed.getTechStack().getDatabases());
        allTech.addAll(parsed.getTechStack().getInfrastructure());
        
        return RepoSummary.builder()
            .repositoryId(repo.getId())
            .userId(userId)
            .repoName(parsed.getRepoName())
            .technologies(allTech)
            .frameworks(parsed.getTechStack().getFrameworks())
            .features(parsed.getProjectMaturity().getFeaturesImplemented())
            .projectType(parsed.getProjectType())
            .complexity(RepoSummary.ComplexityLevel.valueOf(parsed.getComplexity().name()))
            .description(parsed.getDescription())
            .keywords(parsed.getProjectMaturity().getEngineeringPatterns())
            .build();
    }
    
    /**
     * Aggregate all parsed README data into a comprehensive profile for AI
     * This is the main aggregation pipeline that combines all non-AI parsed data
     */
    private AggregatedProfileDTO aggregateParsedReadmes(String userId, List<ParsedReadmeDTO> parsedReadmes, List<Repository> repos) {
        // Tech stack aggregation
        Set<String> languages = new HashSet<>();
        Set<String> frameworks = new HashSet<>();
        Set<String> databases = new HashSet<>();
        Set<String> infrastructure = new HashSet<>();
        Set<String> authentication = new HashSet<>();
        Set<String> tools = new HashSet<>();
        
        // Features and patterns
        Set<String> projectTypes = new HashSet<>();
        Set<String> features = new HashSet<>();
        Set<String> engineeringPatterns = new HashSet<>();
        
        // Distribution tracking
        Map<String, Integer> languageCount = new HashMap<>();
        Map<String, Integer> complexityCount = new HashMap<>();
        Map<String, Integer> projectTypeCount = new HashMap<>();
        
        // Engineering metrics
        int reposWithTests = 0;
        int reposWithCiCd = 0;
        int reposWithEnvExample = 0;
        int reposWithApiDocs = 0;
        int reposWithArchitectureDocs = 0;
        int reposWithContributing = 0;
        int totalSetupQualityScore = 0;
        int totalDocSections = 0;
        Map<String, Integer> qualityIndicatorCounts = new HashMap<>();
        
        // Vibe coding metrics
        int totalVibeScore = 0;
        int rapidPrototypingRepos = 0;
        int structuredRepos = 0;
        Map<String, Integer> vibeSignalCounts = new HashMap<>();
        Map<String, Integer> disciplinedSignalCounts = new HashMap<>();
        
        // Totals
        int totalStars = 0;
        int reposWithReadme = 0;
        int totalReadmeLength = 0;
        
        // Repo snapshots for context
        List<AggregatedProfileDTO.RepoSnapshot> repoSnapshots = new ArrayList<>();
        
        // Process each parsed README
        for (int i = 0; i < parsedReadmes.size(); i++) {
            ParsedReadmeDTO parsed = parsedReadmes.get(i);
            Repository repo = repos.get(i);
            
            // Aggregate tech stack
            ParsedReadmeDTO.TechStack tech = parsed.getTechStack();
            languages.addAll(tech.getLanguages());
            frameworks.addAll(tech.getFrameworks());
            databases.addAll(tech.getDatabases());
            infrastructure.addAll(tech.getInfrastructure());
            authentication.addAll(tech.getAuthentication());
            tools.addAll(tech.getTools());
            
            // Count languages for distribution
            for (String lang : tech.getLanguages()) {
                languageCount.merge(lang, 1, Integer::sum);
            }
            
            // Aggregate features and patterns
            ParsedReadmeDTO.ProjectMaturity maturity = parsed.getProjectMaturity();
            features.addAll(maturity.getFeaturesImplemented());
            engineeringPatterns.addAll(maturity.getEngineeringPatterns());
            
            // Track project types
            projectTypes.add(parsed.getProjectType());
            projectTypeCount.merge(parsed.getProjectType(), 1, Integer::sum);
            
            // Track complexity
            complexityCount.merge(parsed.getComplexity().name(), 1, Integer::sum);
            
            // Aggregate engineering discipline
            ParsedReadmeDTO.EngineeringDiscipline discipline = parsed.getEngineeringDiscipline();
            if (discipline.isHasTests()) reposWithTests++;
            if (discipline.isHasCiCd()) reposWithCiCd++;
            if (discipline.isHasEnvExample()) reposWithEnvExample++;
            if (maturity.isHasApiDocumentation()) reposWithApiDocs++;
            if (maturity.isHasArchitectureDiagram()) reposWithArchitectureDocs++;
            if (discipline.isHasContributing()) reposWithContributing++;
            totalSetupQualityScore += discipline.getSetupQualityScore();
            totalDocSections += discipline.getDocumentationSections().size();
            
            for (String indicator : discipline.getQualityIndicators()) {
                qualityIndicatorCounts.merge(indicator, 1, Integer::sum);
            }
            
            // Aggregate vibe coding analysis
            ParsedReadmeDTO.VibeCodingAnalysis vibe = parsed.getVibeCodingAnalysis();
            totalVibeScore += vibe.getVibeScore();
            if (vibe.getVibeScore() >= 60) rapidPrototypingRepos++;
            if (vibe.getVibeScore() <= 40) structuredRepos++;
            
            for (String signal : vibe.getVibeSignals()) {
                vibeSignalCounts.merge(signal, 1, Integer::sum);
            }
            for (String signal : vibe.getDisciplinedSignals()) {
                disciplinedSignalCounts.merge(signal, 1, Integer::sum);
            }
            
            // Track README stats
            if (parsed.isHasReadme()) reposWithReadme++;
            totalReadmeLength += parsed.getReadmeLength();
            totalStars += repo.getStargazersCount();
            
            // Create repo snapshot
            List<String> mainTech = new ArrayList<>();
            mainTech.addAll(tech.getFrameworks().subList(0, Math.min(2, tech.getFrameworks().size())));
            mainTech.addAll(tech.getDatabases().subList(0, Math.min(1, tech.getDatabases().size())));
            
            repoSnapshots.add(AggregatedProfileDTO.RepoSnapshot.builder()
                .name(parsed.getRepoName())
                .projectType(parsed.getProjectType())
                .complexity(parsed.getComplexity().name())
                .mainTechnologies(mainTech)
                .keyFeatures(maturity.getFeaturesImplemented().subList(0, Math.min(3, maturity.getFeaturesImplemented().size())))
                .vibeScore(vibe.getVibeScore())
                .codingStyle(vibe.getCodingStyle())
                .hasTests(discipline.isHasTests())
                .hasCiCd(discipline.isHasCiCd())
                .build());
        }
        
        int repoCount = parsedReadmes.size();
        
        // Build engineering metrics
        AggregatedProfileDTO.EngineeringMetrics engineeringMetrics = AggregatedProfileDTO.EngineeringMetrics.builder()
            .reposWithTests(reposWithTests)
            .reposWithCiCd(reposWithCiCd)
            .reposWithEnvExample(reposWithEnvExample)
            .reposWithApiDocs(reposWithApiDocs)
            .reposWithArchitectureDocs(reposWithArchitectureDocs)
            .reposWithContributing(reposWithContributing)
            .avgSetupQualityScore(repoCount > 0 ? totalSetupQualityScore / repoCount : 0)
            .avgDocumentationSections(repoCount > 0 ? totalDocSections / repoCount : 0)
            .commonQualityIndicators(getTopItems(qualityIndicatorCounts, 5))
            .build();
        
        // Determine overall coding style
        int avgVibeScore = repoCount > 0 ? totalVibeScore / repoCount : 50;
        String overallCodingStyle;
        if (avgVibeScore >= 60) {
            overallCodingStyle = "Rapid Prototyping Focus";
        } else if (avgVibeScore >= 45) {
            overallCodingStyle = "Balanced Approach";
        } else {
            overallCodingStyle = "Engineering-Focused";
        }
        
        // Build vibe coding metrics
        AggregatedProfileDTO.VibeCodingMetrics vibeCodingMetrics = AggregatedProfileDTO.VibeCodingMetrics.builder()
            .avgVibeScore(avgVibeScore)
            .overallCodingStyle(overallCodingStyle)
            .commonVibeSignals(getTopItems(vibeSignalCounts, 5))
            .commonDisciplinedSignals(getTopItems(disciplinedSignalCounts, 5))
            .rapidPrototypingRepos(rapidPrototypingRepos)
            .structuredRepos(structuredRepos)
            .build();
        
        // Detect potential gaps
        List<String> potentialGaps = detectGapsFromParsedData(
            frameworks, databases, infrastructure, authentication, features, engineeringPatterns,
            reposWithTests, reposWithCiCd, repoCount
        );
        
        return AggregatedProfileDTO.builder()
            .userId(userId)
            .languages(new ArrayList<>(languages))
            .frameworks(new ArrayList<>(frameworks))
            .databases(new ArrayList<>(databases))
            .infrastructure(new ArrayList<>(infrastructure))
            .authentication(new ArrayList<>(authentication))
            .tools(new ArrayList<>(tools))
            .projectTypes(new ArrayList<>(projectTypes))
            .features(new ArrayList<>(features))
            .engineeringPatterns(new ArrayList<>(engineeringPatterns))
            .languageDistribution(languageCount)
            .complexityDistribution(complexityCount)
            .projectTypeDistribution(projectTypeCount)
            .engineeringMetrics(engineeringMetrics)
            .vibeCodingMetrics(vibeCodingMetrics)
            .totalProjects(repoCount)
            .totalStars(totalStars)
            .reposWithReadme(reposWithReadme)
            .avgReadmeLength(repoCount > 0 ? totalReadmeLength / repoCount : 0)
            .potentialGaps(potentialGaps)
            .repoSnapshots(repoSnapshots)
            .build();
    }
    
    /**
     * Get top N items from a count map
     */
    private List<String> getTopItems(Map<String, Integer> countMap, int limit) {
        return countMap.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Enhanced gap detection using parsed README data
     */
    private List<String> detectGapsFromParsedData(
            Set<String> frameworks, Set<String> databases, Set<String> infrastructure,
            Set<String> authentication, Set<String> features, Set<String> engineeringPatterns,
            int reposWithTests, int reposWithCiCd, int totalRepos) {
        
        List<String> gaps = new ArrayList<>();
        
        // Check for backend/frontend balance
        boolean hasBackend = frameworks.stream().anyMatch(f -> 
            f.toLowerCase().contains("spring") || f.toLowerCase().contains("express") || 
            f.toLowerCase().contains("django") || f.toLowerCase().contains("flask") ||
            f.toLowerCase().contains("nest") || f.toLowerCase().contains("fastapi"));
        boolean hasFrontend = frameworks.stream().anyMatch(f ->
            f.toLowerCase().contains("react") || f.toLowerCase().contains("vue") ||
            f.toLowerCase().contains("angular") || f.toLowerCase().contains("next") ||
            f.toLowerCase().contains("svelte"));
        
        // Check for testing
        float testingRatio = totalRepos > 0 ? (float) reposWithTests / totalRepos : 0;
        if (testingRatio < 0.3) {
            gaps.add("Testing/TDD (only " + Math.round(testingRatio * 100) + "% of repos mention tests)");
        }
        
        // Check for CI/CD
        float cicdRatio = totalRepos > 0 ? (float) reposWithCiCd / totalRepos : 0;
        if (cicdRatio < 0.2) {
            gaps.add("CI/CD Pipelines (only " + Math.round(cicdRatio * 100) + "% of repos have CI/CD)");
        }
        
        // Check for containerization
        boolean hasContainerization = infrastructure.stream().anyMatch(i -> 
            i.toLowerCase().contains("docker") || i.toLowerCase().contains("kubernetes"));
        if (!hasContainerization) {
            gaps.add("Containerization (Docker/Kubernetes)");
        }
        
        // Check for cloud
        boolean hasCloud = infrastructure.stream().anyMatch(i -> 
            i.toLowerCase().contains("aws") || i.toLowerCase().contains("azure") || 
            i.toLowerCase().contains("gcp") || i.toLowerCase().contains("cloud"));
        if (!hasCloud) {
            gaps.add("Cloud Deployment (AWS/Azure/GCP)");
        }
        
        // Check for database diversity
        if (databases.isEmpty()) {
            gaps.add("Database Integration");
        } else if (databases.size() == 1) {
            gaps.add("Database Diversity (only " + databases.iterator().next() + " detected)");
        }
        
        // Check for security practices
        if (authentication.isEmpty()) {
            gaps.add("Authentication/Authorization Implementation");
        }
        
        // Check for API documentation
        boolean hasApiDocs = features.contains("api_development") || 
            engineeringPatterns.stream().anyMatch(p -> p.toLowerCase().contains("swagger") || p.toLowerCase().contains("openapi"));
        if (hasBackend && !hasApiDocs) {
            gaps.add("API Documentation (Swagger/OpenAPI)");
        }
        
        // Check for frontend/backend balance
        if (hasBackend && !hasFrontend) gaps.add("Frontend Development");
        if (hasFrontend && !hasBackend) gaps.add("Backend Development");
        
        // Check for monitoring
        boolean hasMonitoring = engineeringPatterns.stream().anyMatch(p -> 
            p.toLowerCase().contains("monitoring") || p.toLowerCase().contains("logging"));
        if (!hasMonitoring && hasBackend) {
            gaps.add("Monitoring & Observability");
        }
        
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
        
        // Generate engineering habit recommendations from vibe coding metrics
        List<AnalysisResult.EngineeringHabitRecommendation> engineeringHabits = new ArrayList<>();
        if (profile.getVibeCodingMetrics() != null && profile.getEngineeringMetrics() != null) {
            AggregatedProfileDTO.EngineeringMetrics engMetrics = profile.getEngineeringMetrics();
            
            // Check testing habits
            if (engMetrics.getReposWithTests() < profile.getTotalProjects() / 2) {
                    engineeringHabits.add(AnalysisResult.EngineeringHabitRecommendation.builder()
                        .habit("testing")
                        .currentState("Tests detected in " + engMetrics.getReposWithTests() + "/" + profile.getTotalProjects() + " repos")
                        .targetState("Include tests in at least 80% of projects")
                        .actionItems(List.of("Start with unit tests for critical functions", "Use TDD for new features", "Add integration tests for APIs"))
                        .priority(1)
                        .build());
                }
                
                // Check CI/CD habits
                if (engMetrics.getReposWithCiCd() < profile.getTotalProjects() / 3) {
                    engineeringHabits.add(AnalysisResult.EngineeringHabitRecommendation.builder()
                        .habit("ci_cd")
                        .currentState("CI/CD detected in " + engMetrics.getReposWithCiCd() + "/" + profile.getTotalProjects() + " repos")
                        .targetState("Set up automated pipelines for all projects")
                        .actionItems(List.of("Start with GitHub Actions for simple builds", "Add automated testing to pipeline", "Consider deployment automation"))
                        .priority(2)
                        .build());
                }
                
                // Check documentation habits
                if (engMetrics.getAvgDocumentationSections() < 4) {
                    engineeringHabits.add(AnalysisResult.EngineeringHabitRecommendation.builder()
                        .habit("documentation")
                        .currentState("Average " + engMetrics.getAvgDocumentationSections() + " documentation sections per README")
                        .targetState("Include setup, usage, API docs, and architecture sections")
                        .actionItems(List.of("Add detailed installation steps", "Document environment variables", "Include architecture diagrams for complex projects"))
                        .priority(3)
                        .build());
                }
        }
        
        // Enhanced recommendations object
        AnalysisResult.EnhancedRecommendations enhancedRecommendations = AnalysisResult.EnhancedRecommendations.builder()
            .skillsToLearn(skillsToLearn)
            .skillsToImprove(skillsToImprove)
            .practiceMore(practiceMore)
            .engineeringHabitsToImprove(engineeringHabits)
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
        
        // Generate coding style assessment from vibe coding metrics
        AnalysisResult.CodingStyleAssessment codingStyleAssessment = null;
        if (profile.getVibeCodingMetrics() != null) {
            AggregatedProfileDTO.VibeCodingMetrics vibeMetrics = profile.getVibeCodingMetrics();
            List<String> styleStrengths = new ArrayList<>();
            List<String> styleAreasToImprove = new ArrayList<>();
            
            // Add disciplined signals as strengths
            if (vibeMetrics.getCommonDisciplinedSignals() != null) {
                styleStrengths.addAll(vibeMetrics.getCommonDisciplinedSignals());
            }
            
            // Add vibe signals as areas to improve
            if (vibeMetrics.getCommonVibeSignals() != null) {
                styleAreasToImprove.addAll(vibeMetrics.getCommonVibeSignals());
            }
            
            String professionalFeedback = generateProfessionalFeedback(vibeMetrics, profile);
            
            codingStyleAssessment = AnalysisResult.CodingStyleAssessment.builder()
                .style(vibeMetrics.getOverallCodingStyle())
                .strengths(styleStrengths)
                .areasToImprove(styleAreasToImprove)
                .professionalFeedback(professionalFeedback)
                .build();
        }
        
        AnalysisResult.DeveloperProfile developerProfile = AnalysisResult.DeveloperProfile.builder()
            .experienceLevel(experienceLevel)
            .primaryLanguages(new ArrayList<>(profile.getLanguages()))
            .primaryFrameworks(new ArrayList<>(profile.getFrameworks()))
            .projectTypes(profile.getProjectTypes())
            .specialization(specialization)
            .codingStyleAssessment(codingStyleAssessment)
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
    
    /**
     * Generate professional feedback about coding style
     * IMPORTANT: Never say "vibe coder" - use professional language
     */
    private String generateProfessionalFeedback(AggregatedProfileDTO.VibeCodingMetrics vibeMetrics, AggregatedProfileDTO profile) {
        int avgVibeScore = vibeMetrics.getAvgVibeScore();
        int structuredRepos = vibeMetrics.getStructuredRepos();
        int rapidRepos = vibeMetrics.getRapidPrototypingRepos();
        int totalRepos = profile.getTotalProjects();
        
        StringBuilder feedback = new StringBuilder();
        
        if (avgVibeScore <= 30) {
            feedback.append("Your repositories demonstrate a strong engineering-focused approach with excellent documentation and testing practices. ");
            feedback.append("This attention to code quality and maintainability is highly valued in professional environments. ");
            feedback.append("Out of ").append(totalRepos).append(" projects, ").append(structuredRepos).append(" show exemplary structure.");
        } else if (avgVibeScore <= 50) {
            feedback.append("Your development approach shows a healthy balance between rapid feature delivery and engineering discipline. ");
            if (structuredRepos > 0) {
                feedback.append("You have ").append(structuredRepos).append(" well-structured project(s) that demonstrate solid engineering practices.");
            }
        } else if (avgVibeScore <= 70) {
            feedback.append("Your repositories suggest a rapid prototyping approach with emphasis on feature building. ");
            if (rapidRepos > 0) {
                feedback.append("Around ").append(rapidRepos).append(" of your ").append(totalRepos).append(" projects could benefit from improved documentation. ");
            }
            feedback.append("Consider investing more time in documentation, testing, and project structure to enhance long-term maintainability.");
        } else {
            feedback.append("The repositories indicate a move-fast development style with limited emphasis on documentation and testing. ");
            feedback.append("While this approach can be effective for quick prototypes, adding structured README files, tests, and CI/CD pipelines ");
            feedback.append("will significantly improve your professional profile and project quality.");
        }
        
        return feedback.toString();
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
