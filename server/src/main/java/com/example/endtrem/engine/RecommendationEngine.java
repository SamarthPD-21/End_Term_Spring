package com.example.endtrem.engine;

import com.example.endtrem.model.RecommendationResult;
import com.example.endtrem.model.RecommendationResult.*;
import com.example.endtrem.model.SkillProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Deterministic Recommendation Engine - Core recommendation generation using rule-based scoring.
 * This component performs gap analysis and generates prioritized, explainable recommendations.
 * NO AI/LLM calls - all decisions are made deterministically based on rules and ontology.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationEngine {
    
    private final SkillOntology ontology;
    private final RoleTemplates roleTemplates;
    
    // Scoring weights
    private static final double WEIGHT_CRITICALITY = 0.30;
    private static final double WEIGHT_GAP_SIZE = 0.20;
    private static final double WEIGHT_MARKET_DEMAND = 0.15;
    private static final double WEIGHT_DEPENDENCY_READY = 0.15;
    private static final double WEIGHT_LEARNING_EFFORT = 0.10;
    private static final double WEIGHT_CAREER_IMPACT = 0.10;
    
    /**
     * Generate recommendations based on skill profile and target role
     */
    public RecommendationResult generateRecommendations(SkillProfile skillProfile, String targetRoleId) {
        log.info("Generating recommendations for user: {} targeting role: {}", 
            skillProfile.getUserId(), targetRoleId);
        
        long startTime = System.currentTimeMillis();
        
        // Convert skill profile to map for easy lookup
        Map<String, Integer> userSkillLevels = convertToSkillMap(skillProfile);
        
        // Determine target role
        String effectiveRoleId = targetRoleId;
        if (effectiveRoleId == null || !roleTemplates.getRole(effectiveRoleId).isPresent()) {
            effectiveRoleId = inferBestRole(userSkillLevels, skillProfile);
        }
        
        RoleTemplates.RoleTemplate targetRole = roleTemplates.getRole(effectiveRoleId)
            .orElse(roleTemplates.getRole("junior_fullstack").get());
        
        // Perform gap analysis
        GapAnalysis gapAnalysis = performGapAnalysis(userSkillLevels, targetRole);
        
        // Generate scored recommendations
        List<ScoredRecommendation> allRecommendations = generateScoredRecommendations(
            gapAnalysis, userSkillLevels, targetRole);
        
        // Categorize recommendations
        CategorizedRecommendations categorized = categorizeRecommendations(allRecommendations);
        
        // Generate engineering habit recommendations
        List<EngineeringHabitRecommendation> engineeringHabits = 
            generateEngineeringHabitRecommendations(skillProfile);
        
        // Build scoring metadata
        long processingTime = System.currentTimeMillis() - startTime;
        ScoringMetadata metadata = ScoringMetadata.builder()
            .scoringVersion("1.0.0")
            .weights(Map.of(
                "criticality", WEIGHT_CRITICALITY,
                "gapSize", WEIGHT_GAP_SIZE,
                "marketDemand", WEIGHT_MARKET_DEMAND,
                "dependencyReady", WEIGHT_DEPENDENCY_READY,
                "learningEffort", WEIGHT_LEARNING_EFFORT,
                "careerImpact", WEIGHT_CAREER_IMPACT
            ))
            .totalSkillsEvaluated(userSkillLevels.size())
            .totalGapsIdentified(gapAnalysis.getCriticalGaps().size() + 
                                 gapAnalysis.getImportantGaps().size() + 
                                 gapAnalysis.getNiceToHaveGaps().size())
            .totalRecommendationsGenerated(allRecommendations.size())
            .processingTimeMs(processingTime)
            .build();
        
        return RecommendationResult.builder()
            .userId(skillProfile.getUserId())
            .skillProfileId(skillProfile.getId())
            .targetRoleId(effectiveRoleId)
            .gapAnalysis(gapAnalysis)
            .recommendations(allRecommendations)
            .categorizedRecommendations(categorized)
            .engineeringHabits(engineeringHabits)
            .scoringMetadata(metadata)
            .build();
    }
    
    // ==================== GAP ANALYSIS ====================
    
    private GapAnalysis performGapAnalysis(Map<String, Integer> userSkills, 
                                           RoleTemplates.RoleTemplate role) {
        List<SkillGap> criticalGaps = new ArrayList<>();
        List<SkillGap> importantGaps = new ArrayList<>();
        List<SkillGap> niceToHaveGaps = new ArrayList<>();
        
        List<SkillMatch> strongMatches = new ArrayList<>();
        List<SkillMatch> adequateMatches = new ArrayList<>();
        List<SkillMatch> partialMatches = new ArrayList<>();
        
        // Analyze critical skills
        for (RoleTemplates.SkillRequirement req : role.getCriticalSkills()) {
            analyzeSkillRequirement(req, userSkills, "CRITICAL", 
                criticalGaps, strongMatches, adequateMatches, partialMatches);
        }
        
        // Analyze important skills
        if (role.getImportantSkills() != null) {
            for (RoleTemplates.SkillRequirement req : role.getImportantSkills()) {
                analyzeSkillRequirement(req, userSkills, "IMPORTANT", 
                    importantGaps, strongMatches, adequateMatches, partialMatches);
            }
        }
        
        // Analyze nice-to-have skills
        if (role.getNiceToHaveSkills() != null) {
            for (RoleTemplates.SkillRequirement req : role.getNiceToHaveSkills()) {
                analyzeSkillRequirement(req, userSkills, "NICE_TO_HAVE", 
                    niceToHaveGaps, strongMatches, adequateMatches, partialMatches);
            }
        }
        
        // Calculate overall readiness
        int totalRequired = role.getCriticalSkills().size() + 
            (role.getImportantSkills() != null ? role.getImportantSkills().size() : 0);
        int totalMatches = strongMatches.size() + adequateMatches.size();
        int overallReadiness = totalRequired > 0 ? (totalMatches * 100) / totalRequired : 0;
        
        return GapAnalysis.builder()
            .targetRole(role.getName())
            .overallReadiness(overallReadiness)
            .criticalGaps(criticalGaps)
            .importantGaps(importantGaps)
            .niceToHaveGaps(niceToHaveGaps)
            .strongMatches(strongMatches)
            .adequateMatches(adequateMatches)
            .partialMatches(partialMatches)
            .build();
    }
    
    private void analyzeSkillRequirement(RoleTemplates.SkillRequirement req,
                                        Map<String, Integer> userSkills,
                                        String importance,
                                        List<SkillGap> gaps,
                                        List<SkillMatch> strong,
                                        List<SkillMatch> adequate,
                                        List<SkillMatch> partial) {
        int userLevel = userSkills.getOrDefault(req.getSkillId(), 0);
        int required = req.getMinimumLevel();
        
        String displayName = ontology.getDisplayName(req.getSkillId());
        String category = ontology.getCategory(req.getSkillId());
        
        if (userLevel >= required + 20) {
            // Strong match - exceeds requirements
            strong.add(SkillMatch.builder()
                .skillName(displayName)
                .category(category)
                .requiredLevel(required)
                .currentLevel(userLevel)
                .surplus(userLevel - required)
                .matchQuality("STRONG")
                .build());
        } else if (userLevel >= required) {
            // Adequate match - meets requirements
            adequate.add(SkillMatch.builder()
                .skillName(displayName)
                .category(category)
                .requiredLevel(required)
                .currentLevel(userLevel)
                .surplus(userLevel - required)
                .matchQuality("ADEQUATE")
                .build());
        } else if (userLevel >= required * 0.6) {
            // Partial match - close but not there
            partial.add(SkillMatch.builder()
                .skillName(displayName)
                .category(category)
                .requiredLevel(required)
                .currentLevel(userLevel)
                .surplus(userLevel - required)
                .matchQuality("PARTIAL")
                .build());
            
            // Also add as gap for improvement
            gaps.add(SkillGap.builder()
                .skillName(displayName)
                .category(category)
                .requiredLevel(required)
                .currentLevel(userLevel)
                .gapSize(required - userLevel)
                .importance(importance)
                .dependencyChain(ontology.getAllPrerequisites(req.getSkillId()))
                .rationale(req.getRationale())
                .build());
        } else {
            // Gap - needs significant work or missing entirely
            gaps.add(SkillGap.builder()
                .skillName(displayName)
                .category(category)
                .requiredLevel(required)
                .currentLevel(userLevel)
                .gapSize(required - userLevel)
                .importance(importance)
                .dependencyChain(ontology.getAllPrerequisites(req.getSkillId()))
                .rationale(req.getRationale())
                .build());
        }
    }
    
    // ==================== RECOMMENDATION GENERATION ====================
    
    private List<ScoredRecommendation> generateScoredRecommendations(
            GapAnalysis gaps, 
            Map<String, Integer> userSkills,
            RoleTemplates.RoleTemplate role) {
        
        List<ScoredRecommendation> recommendations = new ArrayList<>();
        
        // Generate recommendations for gaps
        for (SkillGap gap : gaps.getCriticalGaps()) {
            recommendations.add(createRecommendation(gap, userSkills, 1.0));
        }
        
        for (SkillGap gap : gaps.getImportantGaps()) {
            recommendations.add(createRecommendation(gap, userSkills, 0.7));
        }
        
        for (SkillGap gap : gaps.getNiceToHaveGaps()) {
            recommendations.add(createRecommendation(gap, userSkills, 0.4));
        }
        
        // Add improvement recommendations for partial matches
        for (SkillMatch partial : gaps.getPartialMatches()) {
            recommendations.add(createImprovementRecommendation(partial, userSkills));
        }
        
        // Add practice recommendations for adequate/strong matches (top skills to maintain)
        int practiceCount = 0;
        for (SkillMatch match : gaps.getStrongMatches()) {
            if (practiceCount >= 3) break;
            recommendations.add(createPracticeRecommendation(match));
            practiceCount++;
        }
        
        // Sort by priority (score)
        recommendations.sort(Comparator.comparingInt(ScoredRecommendation::getTotalScore).reversed());
        
        // Assign priorities based on sorted order
        for (int i = 0; i < recommendations.size(); i++) {
            recommendations.get(i).setPriority(i + 1);
        }
        
        return recommendations;
    }
    
    private ScoredRecommendation createRecommendation(SkillGap gap, 
                                                      Map<String, Integer> userSkills,
                                                      double criticalityMultiplier) {
        String skillId = ontology.normalizeSkillName(gap.getSkillName());
        Optional<SkillOntology.SkillNode> skillNode = ontology.getSkill(skillId);
        
        // Calculate component scores
        int criticalityScore = (int)(getImportanceScore(gap.getImportance()) * criticalityMultiplier);
        int gapSizeScore = Math.min(100, gap.getGapSize());
        int marketDemandScore = skillNode.map(SkillOntology.SkillNode::getMarketDemand).orElse(50);
        int dependencyReadyScore = calculateDependencyReadiness(gap.getDependencyChain(), userSkills);
        int learningEffortScore = 100 - skillNode.map(SkillOntology.SkillNode::getBaseDifficulty).orElse(50);
        int careerImpactScore = calculateCareerImpact(gap.getImportance(), marketDemandScore);
        
        // Calculate total weighted score
        int totalScore = (int)(
            criticalityScore * WEIGHT_CRITICALITY +
            gapSizeScore * WEIGHT_GAP_SIZE +
            marketDemandScore * WEIGHT_MARKET_DEMAND +
            dependencyReadyScore * WEIGHT_DEPENDENCY_READY +
            learningEffortScore * WEIGHT_LEARNING_EFFORT +
            careerImpactScore * WEIGHT_CAREER_IMPACT
        );
        
        Map<String, Integer> scoreBreakdown = new LinkedHashMap<>();
        scoreBreakdown.put("criticality", criticalityScore);
        scoreBreakdown.put("gapSize", gapSizeScore);
        scoreBreakdown.put("marketDemand", marketDemandScore);
        scoreBreakdown.put("dependencyReady", dependencyReadyScore);
        scoreBreakdown.put("learningEffort", learningEffortScore);
        scoreBreakdown.put("careerImpact", careerImpactScore);
        
        String category = gap.getCurrentLevel() > 0 ? "improve_existing" : "learn_new";
        String difficultyLevel = getDifficultyLevel(skillNode);
        String estimatedTime = skillNode.map(SkillOntology.SkillNode::getLearningTime).orElse("2-4 weeks");
        
        List<String> prerequisites = gap.getDependencyChain().stream()
            .filter(prereq -> userSkills.getOrDefault(prereq, 0) < 40)
            .map(ontology::getDisplayName)
            .collect(Collectors.toList());
        
        List<String> unlocks = ontology.getAllUnlocks(skillId).stream()
            .limit(3)
            .map(ontology::getDisplayName)
            .collect(Collectors.toList());
        
        return ScoredRecommendation.builder()
            .skill(gap.getSkillName())
            .category(category)
            .priority(0) // Will be set after sorting
            .totalScore(totalScore)
            .scoreBreakdown(scoreBreakdown)
            .reason(buildReason(gap, category))
            .prerequisites(prerequisites)
            .unlocks(unlocks)
            .difficultyLevel(difficultyLevel)
            .estimatedTime(estimatedTime)
            .careerImpact(getCareerImpactLabel(careerImpactScore))
            .resources(getResources(skillId, gap.getSkillName()))
            .evidenceBasis(gap.getRationale())
            .build();
    }
    
    private ScoredRecommendation createImprovementRecommendation(SkillMatch match, 
                                                                  Map<String, Integer> userSkills) {
        String skillId = ontology.normalizeSkillName(match.getSkillName());
        
        int gapSize = match.getRequiredLevel() - match.getCurrentLevel();
        int totalScore = 50 + (gapSize / 2); // Base score plus gap-based adjustment
        
        return ScoredRecommendation.builder()
            .skill(match.getSkillName())
            .category("improve_existing")
            .priority(0)
            .totalScore(totalScore)
            .scoreBreakdown(Map.of("partialMatch", totalScore))
            .reason(String.format("You have foundational %s skills (level %d). Advancing to level %d will meet role requirements.",
                match.getSkillName(), match.getCurrentLevel(), match.getRequiredLevel()))
            .prerequisites(new ArrayList<>())
            .unlocks(ontology.getAllUnlocks(skillId).stream().limit(2).map(ontology::getDisplayName).collect(Collectors.toList()))
            .difficultyLevel("intermediate")
            .estimatedTime("1-2 weeks")
            .careerImpact("medium")
            .resources(getResources(skillId, match.getSkillName()))
            .evidenceBasis("Partial skill match requiring improvement")
            .build();
    }
    
    @SuppressWarnings("unused")
    private ScoredRecommendation createPracticeRecommendation(SkillMatch match) {
        @SuppressWarnings("unused")
        String skillId = ontology.normalizeSkillName(match.getSkillName());
        
        return ScoredRecommendation.builder()
            .skill(match.getSkillName())
            .category("practice_more")
            .priority(0)
            .totalScore(30) // Lower priority for practice
            .scoreBreakdown(Map.of("strongMatch", 30))
            .reason(String.format("You're proficient in %s (level %d). Continue practicing to maintain expertise and explore advanced patterns.",
                match.getSkillName(), match.getCurrentLevel()))
            .prerequisites(new ArrayList<>())
            .unlocks(new ArrayList<>())
            .difficultyLevel("advanced")
            .estimatedTime("Ongoing")
            .careerImpact("medium")
            .resources(List.of(
                "Open source " + match.getSkillName() + " projects",
                match.getSkillName() + " advanced patterns",
                "Build production-grade applications"
            ))
            .evidenceBasis("Strong skill match - maintain and advance")
            .build();
    }
    
    // ==================== CATEGORIZATION ====================
    
    private CategorizedRecommendations categorizeRecommendations(List<ScoredRecommendation> all) {
        List<ScoredRecommendation> toLearn = new ArrayList<>();
        List<ScoredRecommendation> toImprove = new ArrayList<>();
        List<ScoredRecommendation> practice = new ArrayList<>();
        
        for (ScoredRecommendation rec : all) {
            switch (rec.getCategory()) {
                case "learn_new" -> toLearn.add(rec);
                case "improve_existing" -> toImprove.add(rec);
                case "practice_more" -> practice.add(rec);
            }
        }
        
        return CategorizedRecommendations.builder()
            .skillsToLearn(toLearn.stream().limit(5).collect(Collectors.toList()))
            .skillsToImprove(toImprove.stream().limit(5).collect(Collectors.toList()))
            .practiceMore(practice.stream().limit(3).collect(Collectors.toList()))
            .build();
    }
    
    // ==================== ENGINEERING HABITS ====================
    
    private List<EngineeringHabitRecommendation> generateEngineeringHabitRecommendations(
            SkillProfile profile) {
        List<EngineeringHabitRecommendation> habits = new ArrayList<>();
        
        if (profile.getEngineeringScores() == null) return habits;
        
        SkillProfile.EngineeringScores scores = profile.getEngineeringScores();
        
        // Testing habit
        if (scores.getTestingScore() < 50) {
            habits.add(EngineeringHabitRecommendation.builder()
                .habit("testing")
                .currentState(String.format("Testing coverage: %.0f%% of repositories", 
                    scores.getTestingCoverage() * 100))
                .targetState("Include tests in at least 80% of projects")
                .actionItems(List.of(
                    "Start with unit tests for critical business logic",
                    "Learn TDD (Test-Driven Development) approach",
                    "Add integration tests for API endpoints",
                    "Set up code coverage reporting"
                ))
                .priority(1)
                .currentScore(scores.getTestingScore())
                .targetScore(80)
                .build());
        }
        
        // CI/CD habit
        if (scores.getCiCdScore() < 40) {
            habits.add(EngineeringHabitRecommendation.builder()
                .habit("ci_cd")
                .currentState(String.format("CI/CD coverage: %.0f%% of repositories", 
                    scores.getCiCdCoverage() * 100))
                .targetState("Automated pipelines for all production projects")
                .actionItems(List.of(
                    "Start with GitHub Actions for automated testing",
                    "Add automated linting and code quality checks",
                    "Implement automated deployment for staging",
                    "Consider deployment automation for production"
                ))
                .priority(2)
                .currentScore(scores.getCiCdScore())
                .targetScore(70)
                .build());
        }
        
        // Documentation habit
        if (scores.getDocumentationScore() < 60) {
            habits.add(EngineeringHabitRecommendation.builder()
                .habit("documentation")
                .currentState(String.format("Documentation score: %d/100", scores.getDocumentationScore()))
                .targetState("Comprehensive READMEs with setup, usage, and API documentation")
                .actionItems(List.of(
                    "Add detailed installation and setup instructions",
                    "Document all environment variables with examples",
                    "Include architecture diagrams for complex projects",
                    "Add API documentation (Swagger/OpenAPI for APIs)"
                ))
                .priority(3)
                .currentScore(scores.getDocumentationScore())
                .targetScore(80)
                .build());
        }
        
        // Security habit
        if (scores.getSecurityAwarenessScore() < 40) {
            habits.add(EngineeringHabitRecommendation.builder()
                .habit("security")
                .currentState("Limited evidence of security practices in projects")
                .targetState("Implement security best practices in all projects")
                .actionItems(List.of(
                    "Always validate and sanitize user input",
                    "Implement proper authentication and authorization",
                    "Use environment variables for secrets (never commit credentials)",
                    "Enable HTTPS and implement CORS properly"
                ))
                .priority(2)
                .currentScore(scores.getSecurityAwarenessScore())
                .targetScore(70)
                .build());
        }
        
        return habits;
    }
    
    // ==================== HELPER METHODS ====================
    
    private Map<String, Integer> convertToSkillMap(SkillProfile profile) {
        Map<String, Integer> skillMap = new HashMap<>();
        
        if (profile.getDetectedSkills() != null) {
            for (SkillProfile.DetectedSkill skill : profile.getDetectedSkills()) {
                skillMap.put(skill.getNormalizedName(), skill.getProficiencyScore());
            }
        }
        
        return skillMap;
    }
    
    private String inferBestRole(Map<String, Integer> userSkills, SkillProfile profile) {
        // Determine specialization from skills
        boolean hasBackend = userSkills.containsKey("springboot") || 
                            userSkills.containsKey("django") ||
                            userSkills.containsKey("express") ||
                            userSkills.containsKey("spring");
        
        boolean hasFrontend = userSkills.containsKey("react") || 
                             userSkills.containsKey("vue") ||
                             userSkills.containsKey("angular");
        
        String specialization = "fullstack";
        if (hasBackend && !hasFrontend) specialization = "backend";
        else if (hasFrontend && !hasBackend) specialization = "frontend";
        
        // Determine experience level from skill proficiency
        int avgProficiency = (int) userSkills.values().stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(40);
        
        String level = "junior";
        if (avgProficiency >= 70) level = "senior";
        else if (avgProficiency >= 55) level = "mid";
        
        // Map to role ID
        String roleId = level + "_" + specialization;
        if (!roleTemplates.getAllRoleIds().contains(roleId)) {
            roleId = "junior_fullstack";
        }
        
        return roleId;
    }
    
    private int getImportanceScore(String importance) {
        return switch (importance) {
            case "CRITICAL" -> 100;
            case "IMPORTANT" -> 70;
            case "NICE_TO_HAVE" -> 40;
            default -> 50;
        };
    }
    
    private int calculateDependencyReadiness(List<String> dependencies, Map<String, Integer> userSkills) {
        if (dependencies == null || dependencies.isEmpty()) return 100;
        
        int totalReady = 0;
        for (String dep : dependencies) {
            int userLevel = userSkills.getOrDefault(dep, 0);
            if (userLevel >= 50) totalReady++;
            else if (userLevel >= 30) totalReady += 0.5;
        }
        
        return (totalReady * 100) / dependencies.size();
    }
    
    private int calculateCareerImpact(String importance, int marketDemand) {
        int baseImpact = switch (importance) {
            case "CRITICAL" -> 80;
            case "IMPORTANT" -> 60;
            case "NICE_TO_HAVE" -> 40;
            default -> 50;
        };
        
        // Adjust by market demand
        return (baseImpact + marketDemand) / 2;
    }
    
    private String getDifficultyLevel(Optional<SkillOntology.SkillNode> node) {
        int difficulty = node.map(SkillOntology.SkillNode::getBaseDifficulty).orElse(50);
        if (difficulty <= 35) return "beginner";
        if (difficulty <= 55) return "intermediate";
        return "advanced";
    }
    
    private String getCareerImpactLabel(int score) {
        if (score >= 70) return "high";
        if (score >= 45) return "medium";
        return "low";
    }
    
    private String buildReason(SkillGap gap, String category) {
        if (category.equals("learn_new")) {
            return String.format("%s is essential for your target role. %s", 
                gap.getSkillName(), gap.getRationale());
        } else {
            return String.format("Your current %s proficiency (level %d) should be improved to level %d. %s",
                gap.getSkillName(), gap.getCurrentLevel(), gap.getRequiredLevel(), gap.getRationale());
        }
    }
    
    private List<String> getResources(String skillId, String skillName) {
        Map<String, List<String>> resourceMap = Map.ofEntries(
            Map.entry("react", List.of("React Official Documentation", "React Course on Udemy", "Build projects with React")),
            Map.entry("springboot", List.of("Spring Boot Documentation", "Spring in Action book", "Baeldung Tutorials")),
            Map.entry("docker", List.of("Docker Official Tutorial", "Docker for Developers course", "Practice with Docker Compose")),
            Map.entry("kubernetes", List.of("Kubernetes.io Tutorials", "CKAD Certification prep", "Kubernetes the Hard Way")),
            Map.entry("aws", List.of("AWS Free Tier experimentation", "AWS Certified Solutions Architect prep", "A Cloud Guru courses")),
            Map.entry("testing", List.of("TDD by Example book", "JUnit/Jest documentation", "Testing best practices guides")),
            Map.entry("cicd", List.of("GitHub Actions documentation", "CI/CD Pipeline tutorials", "DevOps Roadmap")),
            Map.entry("typescript", List.of("TypeScript Handbook", "TypeScript Deep Dive book", "Migrate JS projects to TS")),
            Map.entry("postgresql", List.of("PostgreSQL Tutorial", "SQL Performance Explained book", "Use PostgreSQL in projects")),
            Map.entry("mongodb", List.of("MongoDB University courses", "MongoDB documentation", "Build MERN stack apps")),
            Map.entry("graphql", List.of("GraphQL.org tutorials", "Apollo GraphQL docs", "Build a GraphQL API")),
            Map.entry("microservices", List.of("Microservices Patterns book", "Building Microservices book", "Design distributed systems"))
        );
        
        return resourceMap.getOrDefault(skillId, List.of(
            "Official " + skillName + " documentation",
            skillName + " tutorials online",
            "Practice with " + skillName + " projects"
        ));
    }
}
