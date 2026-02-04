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
    private final DeterministicRecommendationService recommendationService; // Replaced AIService
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
        
        // Step 5: PIPELINE STAGE 3 - Deterministic recommendation engine (NO AI/OpenAI calls)
        // Uses rule-based scoring, skill ontology, and role templates for gap analysis
        AnalysisResult result = recommendationService.analyzeProfile(aggregatedProfile);
        
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
    
    /**
     * Delete a specific analysis by ID
     */
    public void deleteAnalysis(String userId, String analysisId) {
        AnalysisResult analysis = analysisResultRepository.findById(analysisId)
            .orElseThrow(() -> new RuntimeException("Analysis not found"));
        
        // Verify ownership
        if (!analysis.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this analysis");
        }
        
        analysisResultRepository.deleteById(analysisId);
        log.info("Deleted analysis {} for user {}", analysisId, userId);
    }
    
    /**
     * Delete all analyses for a user
     */
    public void deleteAllAnalyses(String userId) {
        analysisResultRepository.deleteByUserId(userId);
        log.info("Deleted all analyses for user {}", userId);
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
