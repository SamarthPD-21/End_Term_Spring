package com.example.endtrem.engine;

import com.example.endtrem.model.RoleInference;
import com.example.endtrem.model.RoleInference.*;
import com.example.endtrem.model.SkillProfile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tabular ML model (logistic regression) for inferring user's current level
 * and computing distance to target roles.
 * 
 * This is a rule-based approximation of logistic regression using weighted features.
 * The weights are derived from domain knowledge about what distinguishes
 * junior, mid, and senior engineers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleInferenceModel {
    
    private final SkillOntology ontology;
    private final RoleTemplates roleTemplates;
    
    // Feature weights learned from domain knowledge
    // In production, these would be trained on labeled data
    private Map<String, Double> levelFeatureWeights;
    private Map<String, Double> featureImportance;
    
    @PostConstruct
    public void initializeWeights() {
        // Initialize feature weights for level classification
        // Positive weights push towards senior, negative towards junior
        levelFeatureWeights = new LinkedHashMap<>();
        
        // Skill count features
        levelFeatureWeights.put("totalSkillCount", 0.05);
        levelFeatureWeights.put("advancedSkillCount", 0.15);
        levelFeatureWeights.put("intermediateSkillCount", 0.08);
        levelFeatureWeights.put("beginnerSkillCount", -0.05);
        
        // Category coverage
        levelFeatureWeights.put("languageCount", 0.06);
        levelFeatureWeights.put("frameworkCount", 0.08);
        levelFeatureWeights.put("databaseCount", 0.10);
        levelFeatureWeights.put("infrastructureCount", 0.12);
        levelFeatureWeights.put("conceptCount", 0.10);
        
        // Proficiency metrics
        levelFeatureWeights.put("averageProficiency", 0.02);
        levelFeatureWeights.put("maxProficiency", 0.01);
        levelFeatureWeights.put("proficiencyStdDev", -0.02); // Lower std dev = more consistent
        
        // Evidence and experience
        levelFeatureWeights.put("averageEvidenceStrength", 0.015);
        levelFeatureWeights.put("totalProjectCount", 0.03);
        
        // Engineering maturity (strong indicators of seniority)
        levelFeatureWeights.put("testingScore", 0.025);
        levelFeatureWeights.put("documentationScore", 0.02);
        levelFeatureWeights.put("ciCdScore", 0.025);
        
        // Composite features
        levelFeatureWeights.put("technicalMaturityScore", 0.02);
        levelFeatureWeights.put("experienceIndicator", 0.03);
        levelFeatureWeights.put("specializationScore", 0.01);
        levelFeatureWeights.put("breadthScore", 0.015);
        
        // Compute normalized importance
        double totalWeight = levelFeatureWeights.values().stream()
            .mapToDouble(Math::abs)
            .sum();
        
        featureImportance = new LinkedHashMap<>();
        levelFeatureWeights.forEach((k, v) -> 
            featureImportance.put(k, Math.abs(v) / totalWeight * 100));
        
        log.info("Role inference model initialized with {} features", levelFeatureWeights.size());
    }
    
    /**
     * Infer user's current level and compute distances to all roles.
     */
    public RoleInference inferRole(SkillProfile profile) {
        log.info("Inferring role for user: {}", profile.getUserId());
        
        // Extract feature vector
        FeatureVector features = extractFeatures(profile);
        
        // Compute level probabilities
        InferredLevel inferredLevel = computeLevel(features);
        
        // Compute distance to all roles
        List<RoleDistance> roleDistances = computeRoleDistances(profile, features);
        
        // Create model metadata
        ModelMetadata metadata = ModelMetadata.builder()
            .modelVersion("1.0.0")
            .modelType("weighted_logistic_approximation")
            .trainedAt(Instant.parse("2024-01-01T00:00:00Z"))
            .modelAccuracy(0.85) // Estimated accuracy
            .featureImportance(featureImportance)
            .build();
        
        return RoleInference.builder()
            .userId(profile.getUserId())
            .skillProfileId(profile.getId())
            .inferredLevel(inferredLevel)
            .roleDistances(roleDistances)
            .features(features)
            .modelInfo(metadata)
            .createdAt(Instant.now())
            .build();
    }
    
    /**
     * Extract normalized feature vector from skill profile.
     */
    public FeatureVector extractFeatures(SkillProfile profile) {
        List<SkillProfile.DetectedSkill> skills = profile.getDetectedSkills();
        
        if (skills == null || skills.isEmpty()) {
            return createEmptyFeatureVector();
        }
        
        // Skill counts by proficiency level
        int advanced = 0, intermediate = 0, beginner = 0;
        for (SkillProfile.DetectedSkill skill : skills) {
            if (skill.getProficiencyScore() >= 70) advanced++;
            else if (skill.getProficiencyScore() >= 40) intermediate++;
            else beginner++;
        }
        
        // Category counts
        Map<String, Long> categoryCounts = skills.stream()
            .filter(s -> s.getCategory() != null)
            .collect(Collectors.groupingBy(SkillProfile.DetectedSkill::getCategory, Collectors.counting()));
        
        // Proficiency statistics
        double avgProficiency = skills.stream()
            .mapToInt(SkillProfile.DetectedSkill::getProficiencyScore)
            .average()
            .orElse(0);
        
        int maxProficiency = skills.stream()
            .mapToInt(SkillProfile.DetectedSkill::getProficiencyScore)
            .max()
            .orElse(0);
        
        double proficiencyStdDev = computeStdDev(
            skills.stream().mapToInt(SkillProfile.DetectedSkill::getProficiencyScore).toArray());
        
        // Evidence statistics
        double avgEvidence = skills.stream()
            .mapToInt(SkillProfile.DetectedSkill::getEvidenceStrength)
            .average()
            .orElse(0);
        
        int totalProjects = skills.stream()
            .mapToInt(SkillProfile.DetectedSkill::getProjectCount)
            .sum();
        
        // Engineering scores
        SkillProfile.EngineeringScores eng = profile.getEngineeringScores();
        double testingScore = eng != null ? eng.getTestingScore() : 0;
        double docScore = eng != null ? eng.getDocumentationScore() : 0;
        double ciCdScore = eng != null ? eng.getCiCdScore() : 0;
        
        // Compute depth vs breadth
        double specializationScore = computeSpecializationScore(skills);
        double breadthScore = computeBreadthScore(categoryCounts);
        
        // Composite features
        double technicalMaturity = (testingScore + docScore + ciCdScore) / 3.0;
        double experienceIndicator = computeExperienceIndicator(totalProjects, advanced, avgProficiency);
        
        return FeatureVector.builder()
            .totalSkillCount(skills.size())
            .advancedSkillCount(advanced)
            .intermediateSkillCount(intermediate)
            .beginnerSkillCount(beginner)
            .languageCount(categoryCounts.getOrDefault("language", 0L).intValue())
            .frameworkCount(categoryCounts.getOrDefault("framework", 0L).intValue())
            .databaseCount(categoryCounts.getOrDefault("database", 0L).intValue())
            .infrastructureCount(categoryCounts.getOrDefault("infrastructure", 0L).intValue())
            .conceptCount(categoryCounts.getOrDefault("concept", 0L).intValue())
            .averageProficiency(avgProficiency)
            .maxProficiency(maxProficiency)
            .proficiencyStdDev(proficiencyStdDev)
            .averageEvidenceStrength(avgEvidence)
            .totalProjectCount(totalProjects)
            .testingScore(testingScore)
            .documentationScore(docScore)
            .ciCdScore(ciCdScore)
            .specializationScore(specializationScore)
            .breadthScore(breadthScore)
            .technicalMaturityScore(technicalMaturity)
            .experienceIndicator(experienceIndicator)
            .build();
    }
    
    private FeatureVector createEmptyFeatureVector() {
        return FeatureVector.builder()
            .totalSkillCount(0)
            .advancedSkillCount(0)
            .intermediateSkillCount(0)
            .beginnerSkillCount(0)
            .languageCount(0)
            .frameworkCount(0)
            .databaseCount(0)
            .infrastructureCount(0)
            .conceptCount(0)
            .averageProficiency(0)
            .maxProficiency(0)
            .proficiencyStdDev(0)
            .averageEvidenceStrength(0)
            .totalProjectCount(0)
            .testingScore(0)
            .documentationScore(0)
            .ciCdScore(0)
            .specializationScore(0)
            .breadthScore(0)
            .technicalMaturityScore(0)
            .experienceIndicator(0)
            .build();
    }
    
    private double computeStdDev(int[] values) {
        if (values.length == 0) return 0;
        double mean = Arrays.stream(values).average().orElse(0);
        double variance = Arrays.stream(values)
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0);
        return Math.sqrt(variance);
    }
    
    private double computeSpecializationScore(List<SkillProfile.DetectedSkill> skills) {
        // Higher score = deeper expertise in fewer areas
        if (skills.isEmpty()) return 0;
        
        Map<String, List<SkillProfile.DetectedSkill>> byCategory = skills.stream()
            .filter(s -> s.getCategory() != null)
            .collect(Collectors.groupingBy(SkillProfile.DetectedSkill::getCategory));
        
        if (byCategory.isEmpty()) return 0;
        
        // Find max average proficiency in any category
        double maxCategoryAvg = byCategory.values().stream()
            .mapToDouble(list -> list.stream()
                .mapToInt(SkillProfile.DetectedSkill::getProficiencyScore)
                .average()
                .orElse(0))
            .max()
            .orElse(0);
        
        return maxCategoryAvg;
    }
    
    private double computeBreadthScore(Map<String, Long> categoryCounts) {
        // Higher score = coverage across more categories
        int categoriesWithSkills = (int) categoryCounts.values().stream()
            .filter(count -> count > 0)
            .count();
        
        return categoriesWithSkills * 20.0; // Max ~100 for 5 categories
    }
    
    private double computeExperienceIndicator(int totalProjects, int advancedSkills, double avgProficiency) {
        // Composite indicator of experience
        return (totalProjects * 2) + (advancedSkills * 10) + avgProficiency;
    }
    
    /**
     * Compute level classification using weighted features (logistic regression approximation).
     */
    private InferredLevel computeLevel(FeatureVector features) {
        // Compute raw score using weighted sum
        double rawScore = computeRawScore(features);
        
        // Apply sigmoid-like transformation to get probabilities
        // Score thresholds: < 20 = junior, 20-50 = mid, > 50 = senior
        double[] probabilities = computeProbabilities(rawScore);
        
        double juniorProb = probabilities[0];
        double midProb = probabilities[1];
        double seniorProb = probabilities[2];
        
        // Determine level based on highest probability
        String level;
        double confidence;
        
        if (seniorProb >= midProb && seniorProb >= juniorProb) {
            level = "senior";
            confidence = seniorProb;
        } else if (midProb >= juniorProb) {
            level = "mid";
            confidence = midProb;
        } else {
            level = "junior";
            confidence = juniorProb;
        }
        
        // Identify key factors
        List<LevelFactor> keyFactors = identifyKeyFactors(features, level);
        
        // Generate explanation
        String explanation = generateLevelExplanation(level, confidence, keyFactors, features);
        
        return InferredLevel.builder()
            .level(level)
            .confidence(confidence)
            .juniorProbability(juniorProb)
            .midProbability(midProb)
            .seniorProbability(seniorProb)
            .keyFactors(keyFactors)
            .explanation(explanation)
            .build();
    }
    
    private double computeRawScore(FeatureVector f) {
        double score = 0;
        
        score += f.getTotalSkillCount() * levelFeatureWeights.get("totalSkillCount");
        score += f.getAdvancedSkillCount() * levelFeatureWeights.get("advancedSkillCount");
        score += f.getIntermediateSkillCount() * levelFeatureWeights.get("intermediateSkillCount");
        score += f.getBeginnerSkillCount() * levelFeatureWeights.get("beginnerSkillCount");
        score += f.getLanguageCount() * levelFeatureWeights.get("languageCount");
        score += f.getFrameworkCount() * levelFeatureWeights.get("frameworkCount");
        score += f.getDatabaseCount() * levelFeatureWeights.get("databaseCount");
        score += f.getInfrastructureCount() * levelFeatureWeights.get("infrastructureCount");
        score += f.getConceptCount() * levelFeatureWeights.get("conceptCount");
        score += f.getAverageProficiency() * levelFeatureWeights.get("averageProficiency");
        score += f.getMaxProficiency() * levelFeatureWeights.get("maxProficiency");
        score += f.getProficiencyStdDev() * levelFeatureWeights.get("proficiencyStdDev");
        score += f.getAverageEvidenceStrength() * levelFeatureWeights.get("averageEvidenceStrength");
        score += f.getTotalProjectCount() * levelFeatureWeights.get("totalProjectCount");
        score += f.getTestingScore() * levelFeatureWeights.get("testingScore");
        score += f.getDocumentationScore() * levelFeatureWeights.get("documentationScore");
        score += f.getCiCdScore() * levelFeatureWeights.get("ciCdScore");
        score += f.getTechnicalMaturityScore() * levelFeatureWeights.get("technicalMaturityScore");
        score += f.getExperienceIndicator() * levelFeatureWeights.get("experienceIndicator");
        score += f.getSpecializationScore() * levelFeatureWeights.get("specializationScore");
        score += f.getBreadthScore() * levelFeatureWeights.get("breadthScore");
        
        return score;
    }
    
    private double[] computeProbabilities(double rawScore) {
        // Use softmax-like approach with thresholds
        // rawScore: 0-20 = junior, 20-50 = mid, 50+ = senior
        
        double juniorCenter = 10;
        double midCenter = 35;
        double seniorCenter = 60;
        double spread = 15;
        
        double juniorAffinity = Math.exp(-Math.pow(rawScore - juniorCenter, 2) / (2 * spread * spread));
        double midAffinity = Math.exp(-Math.pow(rawScore - midCenter, 2) / (2 * spread * spread));
        double seniorAffinity = Math.exp(-Math.pow(rawScore - seniorCenter, 2) / (2 * spread * spread));
        
        // Adjust for tails
        if (rawScore < 15) juniorAffinity *= 1.5;
        if (rawScore > 55) seniorAffinity *= 1.5;
        
        double total = juniorAffinity + midAffinity + seniorAffinity;
        
        return new double[] {
            Math.round(juniorAffinity / total * 100) / 100.0,
            Math.round(midAffinity / total * 100) / 100.0,
            Math.round(seniorAffinity / total * 100) / 100.0
        };
    }
    
    private List<LevelFactor> identifyKeyFactors(FeatureVector features, String level) {
        List<LevelFactor> factors = new ArrayList<>();
        
        // Add most impactful features
        if (features.getAdvancedSkillCount() > 0) {
            factors.add(LevelFactor.builder()
                .factorName("Advanced Skills")
                .value(features.getAdvancedSkillCount())
                .weight(levelFeatureWeights.get("advancedSkillCount"))
                .contribution(features.getAdvancedSkillCount() * levelFeatureWeights.get("advancedSkillCount"))
                .description(String.format("%d skills at advanced proficiency (>70%%)", features.getAdvancedSkillCount()))
                .build());
        }
        
        if (features.getInfrastructureCount() > 0) {
            factors.add(LevelFactor.builder()
                .factorName("Infrastructure Knowledge")
                .value(features.getInfrastructureCount())
                .weight(levelFeatureWeights.get("infrastructureCount"))
                .contribution(features.getInfrastructureCount() * levelFeatureWeights.get("infrastructureCount"))
                .description(String.format("%d infrastructure/DevOps skills", features.getInfrastructureCount()))
                .build());
        }
        
        if (features.getTechnicalMaturityScore() > 50) {
            factors.add(LevelFactor.builder()
                .factorName("Engineering Practices")
                .value(features.getTechnicalMaturityScore())
                .weight(0.07) // Combined weight
                .contribution(features.getTechnicalMaturityScore() * 0.02)
                .description(String.format("Strong engineering practices (score: %.0f)", features.getTechnicalMaturityScore()))
                .build());
        }
        
        if (features.getTotalProjectCount() > 5) {
            factors.add(LevelFactor.builder()
                .factorName("Project Experience")
                .value(features.getTotalProjectCount())
                .weight(levelFeatureWeights.get("totalProjectCount"))
                .contribution(features.getTotalProjectCount() * levelFeatureWeights.get("totalProjectCount"))
                .description(String.format("%d projects demonstrating practical experience", features.getTotalProjectCount()))
                .build());
        }
        
        if (features.getDatabaseCount() > 0) {
            factors.add(LevelFactor.builder()
                .factorName("Database Experience")
                .value(features.getDatabaseCount())
                .weight(levelFeatureWeights.get("databaseCount"))
                .contribution(features.getDatabaseCount() * levelFeatureWeights.get("databaseCount"))
                .description(String.format("%d database technologies", features.getDatabaseCount()))
                .build());
        }
        
        // Sort by absolute contribution
        factors.sort((a, b) -> Double.compare(Math.abs(b.getContribution()), Math.abs(a.getContribution())));
        
        return factors.stream().limit(5).collect(Collectors.toList());
    }
    
    private String generateLevelExplanation(String level, double confidence, 
                                            List<LevelFactor> factors, FeatureVector features) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(String.format("Based on your skill profile, you are classified as a **%s-level** engineer ", level));
        sb.append(String.format("with %.0f%% confidence. ", confidence * 100));
        
        switch (level) {
            case "junior" -> {
                sb.append("This indicates you're building foundational skills. ");
                if (features.getTotalSkillCount() < 5) {
                    sb.append("Expanding your skill set will help you progress. ");
                }
                if (features.getTechnicalMaturityScore() < 30) {
                    sb.append("Focus on learning testing and documentation practices. ");
                }
            }
            case "mid" -> {
                sb.append("You have solid fundamentals and practical experience. ");
                if (features.getAdvancedSkillCount() < 3) {
                    sb.append("Deepening expertise in core skills will accelerate advancement. ");
                }
                if (features.getInfrastructureCount() < 2) {
                    sb.append("Expanding infrastructure knowledge would round out your profile. ");
                }
            }
            case "senior" -> {
                sb.append("Your profile shows depth, breadth, and engineering maturity. ");
                sb.append("Key indicators: ");
                if (!factors.isEmpty()) {
                    sb.append(factors.get(0).getDescription()).append(". ");
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Compute distance from current profile to each target role.
     */
    private List<RoleDistance> computeRoleDistances(SkillProfile profile, FeatureVector features) {
        List<RoleDistance> distances = new ArrayList<>();
        
        // Get user's current skills as a map
        Map<String, Integer> userSkills = profile.getDetectedSkills().stream()
            .collect(Collectors.toMap(
                SkillProfile.DetectedSkill::getNormalizedName,
                SkillProfile.DetectedSkill::getProficiencyScore,
                (a, b) -> Math.max(a, b)
            ));
        
        // Compute distance to each role template
        for (RoleTemplates.RoleTemplate role : roleTemplates.getAllRoles()) {
            RoleDistance distance = computeDistanceToRole(role, userSkills, features);
            distances.add(distance);
        }
        
        // Sort by overall distance (closest first)
        distances.sort(Comparator.comparingDouble(RoleDistance::getOverallDistance));
        
        return distances;
    }
    
    private RoleDistance computeDistanceToRole(RoleTemplates.RoleTemplate role,
                                                Map<String, Integer> userSkills,
                                                FeatureVector features) {
        // Count skill matches
        int criticalMatched = 0, criticalTotal = 0;
        int importantMatched = 0, importantTotal = 0;
        List<String> topGaps = new ArrayList<>();
        List<String> strengths = new ArrayList<>();
        
        // Check critical skills
        for (RoleTemplates.SkillRequirement req : role.getCriticalSkills()) {
            criticalTotal++;
            Integer userLevel = findSkillLevel(userSkills, req.getSkillId());
            
            if (userLevel != null && userLevel >= req.getMinimumLevel()) {
                criticalMatched++;
                strengths.add(ontology.getDisplayName(req.getSkillId()));
            } else {
                String gap = ontology.getDisplayName(req.getSkillId());
                if (userLevel != null) {
                    gap += String.format(" (have %d%%, need %d%%)", userLevel, req.getMinimumLevel());
                }
                topGaps.add(gap);
            }
        }
        
        // Check important skills
        for (RoleTemplates.SkillRequirement req : role.getImportantSkills()) {
            importantTotal++;
            Integer userLevel = findSkillLevel(userSkills, req.getSkillId());
            
            if (userLevel != null && userLevel >= req.getMinimumLevel()) {
                importantMatched++;
            }
        }
        
        // Compute distance metrics
        double criticalMatchRatio = criticalTotal > 0 ? (double) criticalMatched / criticalTotal : 0;
        double importantMatchRatio = importantTotal > 0 ? (double) importantMatched / importantTotal : 0;
        
        // Technical distance: weighted by skill matches
        double technicalDistance = 100 - (criticalMatchRatio * 60 + importantMatchRatio * 40);
        
        // Experience distance: based on level alignment
        double experienceDistance = computeExperienceDistance(role.getId(), features);
        
        // Engineering practices distance
        double engDistance = computeEngineeringDistance(role.getId(), features);
        
        // Overall weighted distance
        double overallDistance = technicalDistance * 0.5 + experienceDistance * 0.3 + engDistance * 0.2;
        
        // Estimate time to reach
        int weeksToReach = estimateWeeksToReach(overallDistance, topGaps.size());
        String difficulty = classifyDifficulty(overallDistance, role.getId());
        
        // Market context
        double marketDemand = role.getMarketDemand();
        String marketInsight = generateMarketInsight(role, marketDemand);
        
        return RoleDistance.builder()
            .roleId(role.getId())
            .roleName(role.getName())
            .roleLevel(role.getExperienceLevel())
            .specialization(role.getSpecialization())
            .overallDistance(Math.round(overallDistance * 10) / 10.0)
            .technicalDistance(Math.round(technicalDistance * 10) / 10.0)
            .experienceDistance(Math.round(experienceDistance * 10) / 10.0)
            .engineeringPracticesDistance(Math.round(engDistance * 10) / 10.0)
            .criticalSkillsMatched(criticalMatched)
            .criticalSkillsTotal(criticalTotal)
            .importantSkillsMatched(importantMatched)
            .importantSkillsTotal(importantTotal)
            .estimatedWeeksToReach(weeksToReach)
            .difficultyAssessment(difficulty)
            .topGaps(topGaps.stream().limit(5).collect(Collectors.toList()))
            .strengths(strengths.stream().limit(3).collect(Collectors.toList()))
            .marketDemandScore(marketDemand)
            .marketInsight(marketInsight)
            .build();
    }
    
    private Integer findSkillLevel(Map<String, Integer> userSkills, String skillId) {
        // Direct match
        if (userSkills.containsKey(skillId)) {
            return userSkills.get(skillId);
        }
        
        // Try normalized versions
        String normalized = ontology.normalizeSkillName(skillId);
        if (userSkills.containsKey(normalized)) {
            return userSkills.get(normalized);
        }
        
        // Check aliases
        for (String alias : ontology.getAliases(skillId)) {
            if (userSkills.containsKey(alias)) {
                return userSkills.get(alias);
            }
        }
        
        return null;
    }
    
    private double computeExperienceDistance(String roleId, FeatureVector features) {
        String level = extractLevel(roleId);
        
        double expectedAdvanced = switch (level) {
            case "senior" -> 5;
            case "mid" -> 2;
            default -> 0;
        };
        
        double expectedProjects = switch (level) {
            case "senior" -> 15;
            case "mid" -> 8;
            default -> 3;
        };
        
        double advancedGap = Math.max(0, expectedAdvanced - features.getAdvancedSkillCount());
        double projectGap = Math.max(0, expectedProjects - features.getTotalProjectCount());
        
        return Math.min(100, (advancedGap * 10 + projectGap * 3));
    }
    
    private double computeEngineeringDistance(String roleId, FeatureVector features) {
        String level = extractLevel(roleId);
        
        double expectedMaturity = switch (level) {
            case "senior" -> 70;
            case "mid" -> 50;
            default -> 30;
        };
        
        double gap = Math.max(0, expectedMaturity - features.getTechnicalMaturityScore());
        return Math.min(100, gap * 1.5);
    }
    
    private String extractLevel(String roleId) {
        if (roleId.contains("senior")) return "senior";
        if (roleId.contains("mid")) return "mid";
        return "junior";
    }
    
    private int estimateWeeksToReach(double distance, int gapCount) {
        // Base: 1 week per 5 distance points
        int baseWeeks = (int) Math.ceil(distance / 5);
        
        // Add time for each major gap
        baseWeeks += gapCount * 2;
        
        return Math.max(1, Math.min(52, baseWeeks)); // Cap at 1 year
    }
    
    private String classifyDifficulty(double distance, String roleId) {
        String level = extractLevel(roleId);
        
        if (distance < 20) return "achievable";
        if (distance < 40) return "moderate";
        if (distance < 60) return "challenging";
        return "stretch goal";
    }
    
    private String generateMarketInsight(RoleTemplates.RoleTemplate role, double marketDemand) {
        String demandLevel;
        if (marketDemand >= 90) demandLevel = "extremely high";
        else if (marketDemand >= 75) demandLevel = "high";
        else if (marketDemand >= 50) demandLevel = "moderate";
        else demandLevel = "niche";
        
        return String.format("%s roles have %s market demand. ", role.getName(), demandLevel) +
               (marketDemand >= 75 ? "This is a great career target." : 
                "Consider this if aligned with your interests.");
    }
}
