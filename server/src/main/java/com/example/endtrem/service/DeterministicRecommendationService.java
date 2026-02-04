package com.example.endtrem.service;

import com.example.endtrem.dto.AggregatedProfileDTO;
import com.example.endtrem.engine.RecommendationEngine;
import com.example.endtrem.engine.RoleTemplates;
import com.example.endtrem.engine.SkillExtractor;
import com.example.endtrem.engine.SkillOntology;
import com.example.endtrem.model.AnalysisResult;
import com.example.endtrem.model.RecommendationResult;
import com.example.endtrem.model.SkillProfile;
import com.example.endtrem.repository.RecommendationResultRepository;
import com.example.endtrem.repository.SkillProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Deterministic Recommendation Service - Replaces OpenAI for all recommendation decisions.
 * 
 * This service:
 * 1. Extracts skill profiles from aggregated README data
 * 2. Performs gap analysis against role templates
 * 3. Generates prioritized recommendations using rule-based scoring
 * 4. Applies market-aware weighting from Phase-2 intelligence layer
 * 5. Persists all results to MongoDB for reproducibility
 * 6. Optionally formats output with LLM (for display only, not decision-making)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeterministicRecommendationService {
    
    private final SkillExtractor skillExtractor;
    private final RecommendationEngine recommendationEngine;
    private final SkillOntology skillOntology;
    private final RoleTemplates roleTemplates;
    private final SkillProfileRepository skillProfileRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final MarketDemandService marketDemandService;
    
    /**
     * Main entry point - Generate complete analysis from aggregated profile
     * This replaces AIService.analyzeProfile() for all decision-making
     */
    public AnalysisResult analyzeProfile(AggregatedProfileDTO profile) {
        return analyzeProfile(profile, null);
    }
    
    /**
     * Generate analysis with optional target role specification
     */
    public AnalysisResult analyzeProfile(AggregatedProfileDTO profile, String targetRoleId) {
        log.info("Starting deterministic analysis for user: {}", profile.getUserId());
        long startTime = System.currentTimeMillis();
        
        // Step 1: Extract skill profile from aggregated data
        SkillProfile skillProfile = skillExtractor.extractSkillProfile(profile.getUserId(), profile);
        skillProfile = skillProfileRepository.save(skillProfile);
        log.info("Skill profile extracted and saved with ID: {}", skillProfile.getId());
        
        // Step 2: Generate recommendations using the deterministic engine
        RecommendationResult recommendations = recommendationEngine.generateRecommendations(
            skillProfile, targetRoleId);
        
        // Step 2.5: Apply market-aware weighting (Phase-2)
        applyMarketWeighting(recommendations);
        
        recommendations = recommendationResultRepository.save(recommendations);
        log.info("Recommendations generated and saved with ID: {}", recommendations.getId());
        
        // Step 3: Generate formatted output (rule-based, no LLM)
        RecommendationResult.FormattedOutput formattedOutput = generateFormattedOutput(
            skillProfile, recommendations, profile);
        recommendations.setFormattedOutput(formattedOutput);
        recommendationResultRepository.save(recommendations);
        
        // Step 4: Convert to AnalysisResult for API compatibility
        AnalysisResult result = convertToAnalysisResult(skillProfile, recommendations, profile);
        
        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Deterministic analysis completed in {}ms for user: {}", 
            processingTime, profile.getUserId());
        
        return result;
    }
    
    /**
     * Get the latest skill profile for a user
     */
    public Optional<SkillProfile> getLatestSkillProfile(String userId) {
        return skillProfileRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get skill profile history for a user
     */
    public List<SkillProfile> getSkillProfileHistory(String userId) {
        return skillProfileRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get the latest recommendations for a user
     */
    public Optional<RecommendationResult> getLatestRecommendations(String userId) {
        return recommendationResultRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get recommendation history for a user
     */
    public List<RecommendationResult> getRecommendationHistory(String userId) {
        return recommendationResultRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Re-run recommendations for an existing skill profile with a different target role
     */
    public RecommendationResult regenerateRecommendations(String skillProfileId, String targetRoleId) {
        SkillProfile profile = skillProfileRepository.findById(skillProfileId)
            .orElseThrow(() -> new RuntimeException("Skill profile not found: " + skillProfileId));
        
        RecommendationResult recommendations = recommendationEngine.generateRecommendations(
            profile, targetRoleId);
        
        return recommendationResultRepository.save(recommendations);
    }
    
    /**
     * Get available target roles
     */
    public List<RoleTemplates.RoleTemplate> getAvailableRoles() {
        return new ArrayList<>(roleTemplates.getAllRoles());
    }
    
    /**
     * Get available roles for a specialization
     */
    public List<RoleTemplates.RoleTemplate> getRolesBySpecialization(String specialization) {
        return roleTemplates.getRolesBySpecialization(specialization);
    }
    
    // ==================== MARKET-AWARE WEIGHTING (Phase-2) ====================
    
    /**
     * Apply market-aware weighting to recommendations.
     * This adjusts scores based on current market demand, growth trends, and salary premiums.
     */
    private void applyMarketWeighting(RecommendationResult recommendations) {
        if (recommendations.getRecommendations() == null) return;
        
        log.debug("Applying market weighting to {} recommendations", 
            recommendations.getRecommendations().size());
        
        for (RecommendationResult.ScoredRecommendation rec : recommendations.getRecommendations()) {
            String skillId = rec.getSkill().toLowerCase().replace(" ", "-");
            
            // Get market-adjusted score
            double originalScore = rec.getTotalScore();
            double adjustedScore = marketDemandService.computeMarketAdjustedScore(skillId, originalScore);
            
            // Get market insight
            String marketInsight = marketDemandService.getMarketInsight(skillId);
            
            // Update score breakdown to include market adjustment
            Map<String, Integer> breakdown = new HashMap<>(rec.getScoreBreakdown());
            int marketBoost = (int) (adjustedScore - originalScore);
            if (marketBoost != 0) {
                breakdown.put("marketDemand", marketBoost);
            }
            rec.setScoreBreakdown(breakdown);
            rec.setTotalScore((int) adjustedScore);
            
            // Enhance reason with market context
            if (!marketInsight.isEmpty() && !marketInsight.contains("not available")) {
                rec.setReason(rec.getReason() + " " + marketInsight);
            }
            
            // Update career impact based on market data
            double demandScore = marketDemandService.getDemandScore(skillId);
            double growthRate = marketDemandService.getGrowthRate(skillId);
            if (demandScore >= 90 && growthRate > 15) {
                rec.setCareerImpact("very_high");
            } else if (demandScore >= 80 || growthRate > 20) {
                rec.setCareerImpact("high");
            }
        }
        
        // Re-sort by adjusted score
        recommendations.getRecommendations().sort(
            (a, b) -> Integer.compare(b.getTotalScore(), a.getTotalScore()));
        
        // Re-assign priorities
        int priority = 1;
        for (RecommendationResult.ScoredRecommendation rec : recommendations.getRecommendations()) {
            rec.setPriority(priority++);
        }
        
        log.debug("Market weighting applied successfully");
    }
    
    // ==================== FORMAT OUTPUT ====================
    
    /**
     * Generate formatted output - rule-based natural language generation
     * This could be enhanced with LLM for polish, but core content is deterministic
     */
    private RecommendationResult.FormattedOutput generateFormattedOutput(
            SkillProfile skillProfile,
            RecommendationResult recommendations,
            AggregatedProfileDTO profile) {
        
        // Executive Summary
        String executiveSummary = generateExecutiveSummary(skillProfile, recommendations, profile);
        
        // Learning Roadmap
        String learningRoadmap = generateLearningRoadmap(recommendations);
        
        // Career Advice
        String careerAdvice = generateCareerAdvice(skillProfile, recommendations, profile);
        
        // Next Milestone
        String nextMilestone = generateNextMilestone(recommendations, profile);
        
        // Quick Wins
        List<String> quickWins = generateQuickWins(recommendations, skillProfile);
        
        // Professional Feedback
        String professionalFeedback = generateProfessionalFeedback(profile, skillProfile);
        
        return RecommendationResult.FormattedOutput.builder()
            .executiveSummary(executiveSummary)
            .learningRoadmap(learningRoadmap)
            .careerAdvice(careerAdvice)
            .nextMilestone(nextMilestone)
            .quickWins(quickWins)
            .professionalFeedback(professionalFeedback)
            .build();
    }
    
    private String generateExecutiveSummary(SkillProfile skillProfile,
                                           RecommendationResult recommendations,
                                           AggregatedProfileDTO profile) {
        StringBuilder sb = new StringBuilder();
        
        RecommendationResult.GapAnalysis gaps = recommendations.getGapAnalysis();
        int readiness = gaps.getOverallReadiness();
        
        sb.append("Based on analysis of ").append(profile.getTotalProjects())
          .append(" repositories, you are ").append(readiness).append("% ready for the ")
          .append(gaps.getTargetRole()).append(" role. ");
        
        if (gaps.getStrongMatches() != null && !gaps.getStrongMatches().isEmpty()) {
            sb.append("You excel in ");
            sb.append(gaps.getStrongMatches().stream()
                .limit(3)
                .map(RecommendationResult.SkillMatch::getSkillName)
                .collect(Collectors.joining(", ")));
            sb.append(". ");
        }
        
        if (gaps.getCriticalGaps() != null && !gaps.getCriticalGaps().isEmpty()) {
            sb.append("Key areas to develop: ");
            sb.append(gaps.getCriticalGaps().stream()
                .limit(3)
                .map(RecommendationResult.SkillGap::getSkillName)
                .collect(Collectors.joining(", ")));
            sb.append(".");
        }
        
        return sb.toString();
    }
    
    private String generateLearningRoadmap(RecommendationResult recommendations) {
        StringBuilder sb = new StringBuilder();
        
        RecommendationResult.CategorizedRecommendations cats = recommendations.getCategorizedRecommendations();
        
        sb.append("**Phase 1 - Priority Skills to Learn:**\n");
        if (cats.getSkillsToLearn() != null && !cats.getSkillsToLearn().isEmpty()) {
            for (int i = 0; i < Math.min(3, cats.getSkillsToLearn().size()); i++) {
                RecommendationResult.ScoredRecommendation rec = cats.getSkillsToLearn().get(i);
                sb.append(i + 1).append(". **").append(rec.getSkill()).append("** - ")
                  .append(rec.getEstimatedTime()).append("\n")
                  .append("   ").append(rec.getReason()).append("\n");
            }
        }
        
        sb.append("\n**Phase 2 - Improve Existing Skills:**\n");
        if (cats.getSkillsToImprove() != null && !cats.getSkillsToImprove().isEmpty()) {
            for (int i = 0; i < Math.min(3, cats.getSkillsToImprove().size()); i++) {
                RecommendationResult.ScoredRecommendation rec = cats.getSkillsToImprove().get(i);
                sb.append(i + 1).append(". **").append(rec.getSkill()).append("** - ")
                  .append(rec.getEstimatedTime()).append("\n");
            }
        }
        
        sb.append("\n**Ongoing - Practice & Maintain:**\n");
        if (cats.getPracticeMore() != null && !cats.getPracticeMore().isEmpty()) {
            for (RecommendationResult.ScoredRecommendation rec : cats.getPracticeMore()) {
                sb.append("- ").append(rec.getSkill()).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    private String generateCareerAdvice(SkillProfile skillProfile,
                                        RecommendationResult recommendations,
                                        AggregatedProfileDTO profile) {
        StringBuilder sb = new StringBuilder();
        
        RecommendationResult.GapAnalysis gaps = recommendations.getGapAnalysis();
        int readiness = gaps.getOverallReadiness();
        
        if (readiness >= 80) {
            sb.append("You're well-positioned for ").append(gaps.getTargetRole())
              .append(" roles. Consider targeting senior positions or specializing deeper in your strongest areas.");
        } else if (readiness >= 60) {
            sb.append("You have a solid foundation. Focus on the critical gaps identified to become a strong candidate for ")
              .append(gaps.getTargetRole()).append(" positions within 2-3 months of dedicated learning.");
        } else if (readiness >= 40) {
            sb.append("You're building good experience. A focused 3-6 month learning plan targeting the key skills will significantly improve your readiness for ")
              .append(gaps.getTargetRole()).append(" roles.");
        } else {
            sb.append("Consider starting with foundational skills first. Building projects while learning will accelerate your path to ")
              .append(gaps.getTargetRole()).append(" readiness.");
        }
        
        // Add engineering habits advice
        if (recommendations.getEngineeringHabits() != null && !recommendations.getEngineeringHabits().isEmpty()) {
            sb.append(" Additionally, improving your ").append(
                recommendations.getEngineeringHabits().stream()
                    .limit(2)
                    .map(RecommendationResult.EngineeringHabitRecommendation::getHabit)
                    .collect(Collectors.joining(" and "))
            ).append(" practices will make you stand out to employers.");
        }
        
        return sb.toString();
    }
    
    private String generateNextMilestone(RecommendationResult recommendations, AggregatedProfileDTO profile) {
        RecommendationResult.CategorizedRecommendations cats = recommendations.getCategorizedRecommendations();
        
        if (cats.getSkillsToLearn() != null && !cats.getSkillsToLearn().isEmpty()) {
            RecommendationResult.ScoredRecommendation topRec = cats.getSkillsToLearn().get(0);
            return "Learn " + topRec.getSkill() + " (" + topRec.getEstimatedTime() + ") - " + 
                   "This has the highest impact on your career progression.";
        }
        
        if (cats.getSkillsToImprove() != null && !cats.getSkillsToImprove().isEmpty()) {
            RecommendationResult.ScoredRecommendation topRec = cats.getSkillsToImprove().get(0);
            return "Deepen your " + topRec.getSkill() + " expertise - Build a more complex project to level up.";
        }
        
        return "Continue building projects and contributing to open source to maintain and expand your skills.";
    }
    
    private List<String> generateQuickWins(RecommendationResult recommendations, SkillProfile skillProfile) {
        List<String> quickWins = new ArrayList<>();
        
        // Engineering habits quick wins
        if (recommendations.getEngineeringHabits() != null) {
            for (RecommendationResult.EngineeringHabitRecommendation habit : recommendations.getEngineeringHabits()) {
                if (habit.getActionItems() != null && !habit.getActionItems().isEmpty()) {
                    quickWins.add(habit.getActionItems().get(0));
                    if (quickWins.size() >= 3) break;
                }
            }
        }
        
        // Add low-effort skill improvements
        if (recommendations.getCategorizedRecommendations().getSkillsToImprove() != null) {
            for (RecommendationResult.ScoredRecommendation rec : 
                    recommendations.getCategorizedRecommendations().getSkillsToImprove()) {
                if ("beginner".equals(rec.getDifficultyLevel()) && quickWins.size() < 5) {
                    quickWins.add("Read " + rec.getSkill() + " documentation for 30 minutes");
                }
            }
        }
        
        // Default quick wins if we don't have enough
        if (quickWins.isEmpty()) {
            quickWins.add("Add a README to your most recent project");
            quickWins.add("Write tests for one function in your latest codebase");
            quickWins.add("Set up a simple GitHub Actions workflow");
        }
        
        return quickWins.stream().limit(5).collect(Collectors.toList());
    }
    
    private String generateProfessionalFeedback(AggregatedProfileDTO profile, SkillProfile skillProfile) {
        StringBuilder fb = new StringBuilder();
        
        if (profile.getVibeCodingMetrics() != null) {
            int avgVibeScore = profile.getVibeCodingMetrics().getAvgVibeScore();
            
            if (avgVibeScore <= 35) {
                fb.append("Your repositories demonstrate a strong engineering discipline with good documentation and testing practices. ");
                fb.append("This attention to code quality is highly valued in professional settings.");
            } else if (avgVibeScore <= 55) {
                fb.append("Your development approach shows a healthy balance between shipping features and maintaining code quality. ");
                fb.append("Consider deepening your testing and documentation practices to further strengthen your profile.");
            } else if (avgVibeScore <= 75) {
                fb.append("Your repositories show a fast-paced development style focused on feature delivery. ");
                fb.append("Adding more documentation, tests, and CI/CD pipelines will significantly enhance your professional profile.");
            } else {
                fb.append("Your rapid prototyping approach is great for exploration and MVPs. ");
                fb.append("For production-ready code, focus on adding comprehensive documentation, testing, and deployment automation.");
            }
        }
        
        // Add skill-specific feedback
        SkillProfile.EngineeringScores scores = skillProfile.getEngineeringScores();
        if (scores != null) {
            if (scores.getTestingScore() >= 70) {
                fb.append(" Your testing practices are commendable.");
            }
            if (scores.getCiCdScore() >= 60) {
                fb.append(" Good CI/CD adoption across your projects.");
            }
        }
        
        return fb.toString();
    }
    
    // ==================== CONVERT TO ANALYSIS RESULT ====================
    
    /**
     * Convert internal models to AnalysisResult for API compatibility
     */
    private AnalysisResult convertToAnalysisResult(SkillProfile skillProfile,
                                                   RecommendationResult recommendations,
                                                   AggregatedProfileDTO profile) {
        // Build skill analysis
        AnalysisResult.SkillAnalysis skillAnalysis = buildSkillAnalysis(skillProfile);
        
        // Build enhanced recommendations
        AnalysisResult.EnhancedRecommendations enhancedRecs = buildEnhancedRecommendations(recommendations);
        
        // Build combined recommendations list
        List<AnalysisResult.LearningRecommendation> allRecs = new ArrayList<>();
        if (recommendations.getCategorizedRecommendations() != null) {
            allRecs.addAll(convertRecommendations(recommendations.getCategorizedRecommendations().getSkillsToLearn()));
            allRecs.addAll(convertRecommendations(recommendations.getCategorizedRecommendations().getSkillsToImprove()));
            allRecs.addAll(convertRecommendations(recommendations.getCategorizedRecommendations().getPracticeMore()));
        }
        
        // Build developer profile
        AnalysisResult.DeveloperProfile developerProfile = buildDeveloperProfile(
            skillProfile, recommendations, profile);
        
        return AnalysisResult.builder()
            .userId(profile.getUserId())
            .skillAnalysis(skillAnalysis)
            .recommendations(allRecs)
            .enhancedRecommendations(enhancedRecs)
            .developerProfile(developerProfile)
            .totalRepositoriesAnalyzed(profile.getTotalProjects())
            .build();
    }
    
    private AnalysisResult.SkillAnalysis buildSkillAnalysis(SkillProfile profile) {
        List<AnalysisResult.Skill> strong = new ArrayList<>();
        List<AnalysisResult.Skill> moderate = new ArrayList<>();
        List<AnalysisResult.Skill> weak = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        
        if (profile.getDetectedSkills() != null) {
            for (SkillProfile.DetectedSkill skill : profile.getDetectedSkills()) {
                AnalysisResult.Skill converted = AnalysisResult.Skill.builder()
                    .name(skill.getName())
                    .category(skill.getCategory())
                    .proficiencyScore(skill.getProficiencyScore())
                    .evidence(skill.getPrimaryEvidence())
                    .projectCount(skill.getProjectCount())
                    .trend(skill.getTrend())
                    .build();
                
                if (skill.getProficiencyScore() >= 70) {
                    strong.add(converted);
                } else if (skill.getProficiencyScore() >= 45) {
                    moderate.add(converted);
                } else {
                    weak.add(converted);
                }
            }
        }
        
        // Sort by proficiency
        strong.sort(Comparator.comparingInt(AnalysisResult.Skill::getProficiencyScore).reversed());
        moderate.sort(Comparator.comparingInt(AnalysisResult.Skill::getProficiencyScore).reversed());
        
        return AnalysisResult.SkillAnalysis.builder()
            .strongSkills(strong)
            .moderateSkills(moderate)
            .weakSkills(weak)
            .missingSkills(missing)
            .totalSkillsCount(strong.size() + moderate.size() + weak.size())
            .build();
    }
    
    private AnalysisResult.EnhancedRecommendations buildEnhancedRecommendations(
            RecommendationResult recommendations) {
        
        List<AnalysisResult.LearningRecommendation> toLearn = new ArrayList<>();
        List<AnalysisResult.LearningRecommendation> toImprove = new ArrayList<>();
        List<AnalysisResult.LearningRecommendation> practice = new ArrayList<>();
        List<AnalysisResult.EngineeringHabitRecommendation> habits = new ArrayList<>();
        
        if (recommendations.getCategorizedRecommendations() != null) {
            toLearn = convertRecommendations(recommendations.getCategorizedRecommendations().getSkillsToLearn());
            toImprove = convertRecommendations(recommendations.getCategorizedRecommendations().getSkillsToImprove());
            practice = convertRecommendations(recommendations.getCategorizedRecommendations().getPracticeMore());
        }
        
        if (recommendations.getEngineeringHabits() != null) {
            for (RecommendationResult.EngineeringHabitRecommendation h : recommendations.getEngineeringHabits()) {
                habits.add(AnalysisResult.EngineeringHabitRecommendation.builder()
                    .habit(h.getHabit())
                    .currentState(h.getCurrentState())
                    .targetState(h.getTargetState())
                    .actionItems(h.getActionItems())
                    .priority(h.getPriority())
                    .build());
            }
        }
        
        List<String> careerAdvice = new ArrayList<>();
        String nextMilestone = "";
        
        if (recommendations.getFormattedOutput() != null) {
            if (recommendations.getFormattedOutput().getCareerAdvice() != null) {
                careerAdvice.add(recommendations.getFormattedOutput().getCareerAdvice());
            }
            if (recommendations.getFormattedOutput().getNextMilestone() != null) {
                nextMilestone = recommendations.getFormattedOutput().getNextMilestone();
            }
        }
        
        return AnalysisResult.EnhancedRecommendations.builder()
            .skillsToLearn(toLearn)
            .skillsToImprove(toImprove)
            .practiceMore(practice)
            .engineeringHabitsToImprove(habits)
            .careerAdvice(careerAdvice)
            .nextMilestone(nextMilestone)
            .build();
    }
    
    private List<AnalysisResult.LearningRecommendation> convertRecommendations(
            List<RecommendationResult.ScoredRecommendation> scored) {
        if (scored == null) return new ArrayList<>();
        
        return scored.stream().map(rec -> AnalysisResult.LearningRecommendation.builder()
            .skill(rec.getSkill())
            .reason(rec.getReason())
            .priority(rec.getPriority())
            .resources(rec.getResources())
            .estimatedTimeToLearn(rec.getEstimatedTime())
            .category(rec.getCategory())
            .relatedSkills(rec.getUnlocks())
            .difficultyLevel(rec.getDifficultyLevel())
            .careerImpact(rec.getCareerImpact())
            .build()
        ).collect(Collectors.toList());
    }
    
    private AnalysisResult.DeveloperProfile buildDeveloperProfile(SkillProfile skillProfile,
                                                                   RecommendationResult recommendations,
                                                                   AggregatedProfileDTO profile) {
        // Determine experience level
        String experienceLevel = "Junior";
        int avgProficiency = skillProfile.getDetectedSkills() != null 
            ? (int) skillProfile.getDetectedSkills().stream()
                .mapToInt(SkillProfile.DetectedSkill::getProficiencyScore)
                .average()
                .orElse(40)
            : 40;
        
        if (avgProficiency >= 70) experienceLevel = "Senior";
        else if (avgProficiency >= 55) experienceLevel = "Mid";
        
        // Get primary languages and frameworks
        List<String> primaryLanguages = profile.getLanguages() != null 
            ? profile.getLanguages().stream().limit(3).collect(Collectors.toList())
            : new ArrayList<>();
        
        List<String> primaryFrameworks = profile.getFrameworks() != null
            ? profile.getFrameworks().stream().limit(3).collect(Collectors.toList())
            : new ArrayList<>();
        
        // Determine specialization
        String specialization = "fullstack";
        boolean hasBackend = primaryFrameworks.stream().anyMatch(f ->
            f.toLowerCase().contains("spring") || f.toLowerCase().contains("express") ||
            f.toLowerCase().contains("django") || f.toLowerCase().contains("flask"));
        boolean hasFrontend = primaryFrameworks.stream().anyMatch(f ->
            f.toLowerCase().contains("react") || f.toLowerCase().contains("vue") ||
            f.toLowerCase().contains("angular"));
        
        if (hasBackend && !hasFrontend) specialization = "backend";
        else if (hasFrontend && !hasBackend) specialization = "frontend";
        
        // Build coding style assessment
        AnalysisResult.CodingStyleAssessment codingStyle = null;
        if (profile.getVibeCodingMetrics() != null) {
            AggregatedProfileDTO.VibeCodingMetrics vibe = profile.getVibeCodingMetrics();
            codingStyle = AnalysisResult.CodingStyleAssessment.builder()
                .style(vibe.getOverallCodingStyle())
                .strengths(vibe.getCommonDisciplinedSignals())
                .areasToImprove(vibe.getCommonVibeSignals())
                .professionalFeedback(recommendations.getFormattedOutput() != null 
                    ? recommendations.getFormattedOutput().getProfessionalFeedback() : "")
                .build();
        }
        
        // Build skill distribution
        Map<String, Integer> skillDistribution = new HashMap<>();
        if (skillProfile.getSkillsByCategory() != null) {
            for (Map.Entry<String, List<SkillProfile.DetectedSkill>> entry : 
                    skillProfile.getSkillsByCategory().entrySet()) {
                skillDistribution.put(entry.getKey(), entry.getValue().size() * 10);
            }
        }
        
        // Get strengths and areas for growth
        List<String> strengths = new ArrayList<>();
        List<String> areasForGrowth = new ArrayList<>();
        
        if (recommendations.getGapAnalysis() != null) {
            for (RecommendationResult.SkillMatch match : recommendations.getGapAnalysis().getStrongMatches()) {
                strengths.add(match.getSkillName());
            }
            for (RecommendationResult.SkillGap gap : recommendations.getGapAnalysis().getCriticalGaps()) {
                areasForGrowth.add(gap.getSkillName());
            }
        }
        
        // Career stage
        String careerStage = switch (experienceLevel) {
            case "Senior" -> "senior";
            case "Mid" -> "mid";
            default -> "junior";
        };
        
        return AnalysisResult.DeveloperProfile.builder()
            .experienceLevel(experienceLevel)
            .primaryLanguages(primaryLanguages)
            .primaryFrameworks(primaryFrameworks)
            .projectTypes(profile.getProjectTypes())
            .specialization(specialization)
            .codingStyleAssessment(codingStyle)
            .skillDistribution(skillDistribution)
            .strengths(strengths.stream().limit(5).collect(Collectors.toList()))
            .areasForGrowth(areasForGrowth.stream().limit(5).collect(Collectors.toList()))
            .careerStage(careerStage)
            .build();
    }
}
