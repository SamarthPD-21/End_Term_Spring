package com.example.endtrem.service;

import com.example.endtrem.engine.RoleInferenceModel;
import com.example.endtrem.engine.SkillProgressionTracker;
import com.example.endtrem.model.*;
import com.example.endtrem.model.RecommendationResult.ScoredRecommendation;
import com.example.endtrem.repository.RoleInferenceRepository;
import com.example.endtrem.repository.SkillProfileRepository;
import com.example.endtrem.repository.SkillProgressionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase-2 Intelligence Layer Service.
 * Provides advanced analytics on top of the deterministic recommendation engine:
 * - Skill progression tracking (improving/stagnating/regressing)
 * - Role inference (current level estimation, distance to goals)
 * - Market-aware recommendation weighting
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntelligenceLayerService {
    
    private final SkillProgressionTracker progressionTracker;
    private final RoleInferenceModel roleInferenceModel;
    private final MarketDemandService marketDemandService;
    
    private final SkillProfileRepository profileRepository;
    private final SkillProgressionRepository progressionRepository;
    private final RoleInferenceRepository inferenceRepository;
    
    // ============================================
    // SKILL PROGRESSION TRACKING
    // ============================================
    
    /**
     * Compute and store progression between the latest and previous profile.
     */
    public Optional<SkillProgression> computeLatestProgression(String userId) {
        log.info("Computing latest progression for user: {}", userId);
        
        List<SkillProfile> profiles = profileRepository
            .findTop10ByUserIdOrderByCreatedAtDesc(userId);
        
        if (profiles.size() < 2) {
            log.info("Not enough profiles for progression analysis. Need at least 2, found: {}", profiles.size());
            return Optional.empty();
        }
        
        SkillProfile current = profiles.get(0);
        SkillProfile previous = profiles.get(1);
        
        SkillProgression progression = progressionTracker.computeProgression(previous, current);
        progression = progressionRepository.save(progression);
        
        log.info("Progression computed: status={}, score={}", 
            progression.getSummary().getOverallStatus(),
            progression.getSummary().getOverallProgressScore());
        
        return Optional.of(progression);
    }
    
    /**
     * Compute progression between any two profiles.
     */
    public SkillProgression computeProgression(String previousProfileId, String currentProfileId) {
        SkillProfile previous = profileRepository.findById(previousProfileId)
            .orElseThrow(() -> new IllegalArgumentException("Previous profile not found: " + previousProfileId));
        
        SkillProfile current = profileRepository.findById(currentProfileId)
            .orElseThrow(() -> new IllegalArgumentException("Current profile not found: " + currentProfileId));
        
        // Check if already computed
        Optional<SkillProgression> existing = progressionRepository
            .findByPreviousProfileIdAndCurrentProfileId(previousProfileId, currentProfileId);
        
        if (existing.isPresent()) {
            return existing.get();
        }
        
        SkillProgression progression = progressionTracker.computeProgression(previous, current);
        return progressionRepository.save(progression);
    }
    
    /**
     * Get the latest progression analysis for a user.
     */
    public Optional<SkillProgression> getLatestProgression(String userId) {
        return progressionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get progression history for a user.
     */
    public List<SkillProgression> getProgressionHistory(String userId) {
        return progressionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Generate a progression summary in natural language.
     */
    public String generateProgressionNarrative(String userId) {
        Optional<SkillProgression> progressionOpt = getLatestProgression(userId);
        
        if (progressionOpt.isEmpty()) {
            return "No progression data available yet. Complete at least two profile analyses to track your progress.";
        }
        
        SkillProgression p = progressionOpt.get();
        StringBuilder narrative = new StringBuilder();
        
        // Overall summary
        narrative.append("## Your Skill Progression\n\n");
        narrative.append(p.getSummary().getNarrativeSummary()).append("\n\n");
        
        // Velocity insights
        if (p.getVelocity() != null) {
            narrative.append("### Learning Velocity\n");
            narrative.append(String.format("- New skills per month: %.1f\n", p.getVelocity().getNewSkillsPerMonth()));
            narrative.append(String.format("- Skill improvements per month: %.1f\n", p.getVelocity().getSkillImprovementsPerMonth()));
            narrative.append(String.format("- Assessment: %s\n", p.getVelocity().getVelocityAssessment()));
            narrative.append(String.format("- Estimated time to next level: %d days\n\n", 
                p.getVelocity().getEstimatedDaysToNextLevel()));
        }
        
        // Top changes
        narrative.append("### Notable Changes\n");
        int shown = 0;
        for (SkillProgression.SkillDelta delta : p.getSkillDeltas()) {
            if (shown >= 5) break;
            if (delta.getDeltaType() != SkillProgression.DeltaType.STABLE) {
                narrative.append("- ").append(delta.getInsight()).append("\n");
                shown++;
            }
        }
        
        // Trends
        if (p.getTrends() != null) {
            if (!p.getTrends().getStrongestGrowthAreas().isEmpty()) {
                narrative.append("\n### Strongest Growth Areas\n");
                for (String area : p.getTrends().getStrongestGrowthAreas()) {
                    narrative.append("- ").append(area).append("\n");
                }
            }
            
            if (!p.getTrends().getDetectedPatterns().isEmpty()) {
                narrative.append("\n### Detected Patterns\n");
                for (String pattern : p.getTrends().getDetectedPatterns()) {
                    narrative.append("- ").append(pattern).append("\n");
                }
            }
        }
        
        return narrative.toString();
    }
    
    // ============================================
    // ROLE INFERENCE
    // ============================================
    
    /**
     * Infer user's current level and compute role distances.
     */
    public RoleInference inferRole(String userId) {
        log.info("Inferring role for user: {}", userId);
        
        Optional<SkillProfile> profileOpt = profileRepository
            .findFirstByUserIdOrderByCreatedAtDesc(userId);
        
        if (profileOpt.isEmpty()) {
            throw new IllegalStateException("No skill profile found for user: " + userId);
        }
        
        SkillProfile profile = profileOpt.get();
        
        // Check if inference already exists for this profile
        Optional<RoleInference> existing = inferenceRepository.findBySkillProfileId(profile.getId());
        if (existing.isPresent()) {
            log.info("Using cached role inference for profile: {}", profile.getId());
            return existing.get();
        }
        
        // Compute new inference
        RoleInference inference = roleInferenceModel.inferRole(profile);
        inference = inferenceRepository.save(inference);
        
        log.info("Role inference complete: level={}, confidence={}", 
            inference.getInferredLevel().getLevel(),
            inference.getInferredLevel().getConfidence());
        
        return inference;
    }
    
    /**
     * Get the latest role inference for a user.
     */
    public Optional<RoleInference> getLatestInference(String userId) {
        return inferenceRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get role inference history.
     */
    public List<RoleInference> getInferenceHistory(String userId) {
        return inferenceRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get distance to a specific target role.
     */
    public Optional<RoleInference.RoleDistance> getDistanceToRole(String userId, String targetRoleId) {
        return getLatestInference(userId)
            .flatMap(inference -> inference.getRoleDistances().stream()
                .filter(d -> d.getRoleId().equals(targetRoleId))
                .findFirst());
    }
    
    /**
     * Get the closest reachable role for a user.
     */
    public Optional<RoleInference.RoleDistance> getClosestRole(String userId) {
        return getLatestInference(userId)
            .flatMap(inference -> inference.getRoleDistances().stream()
                .min(Comparator.comparingDouble(RoleInference.RoleDistance::getOverallDistance)));
    }
    
    /**
     * Generate role inference narrative.
     */
    public String generateRoleNarrative(String userId) {
        Optional<RoleInference> inferenceOpt = getLatestInference(userId);
        
        if (inferenceOpt.isEmpty()) {
            return "No role inference available. Complete a profile analysis first.";
        }
        
        RoleInference inference = inferenceOpt.get();
        StringBuilder narrative = new StringBuilder();
        
        // Current level
        RoleInference.InferredLevel level = inference.getInferredLevel();
        narrative.append("## Your Role Analysis\n\n");
        narrative.append(level.getExplanation()).append("\n\n");
        
        // Probability breakdown
        narrative.append("### Level Probabilities\n");
        narrative.append(String.format("- Junior: %.0f%%\n", level.getJuniorProbability() * 100));
        narrative.append(String.format("- Mid-level: %.0f%%\n", level.getMidProbability() * 100));
        narrative.append(String.format("- Senior: %.0f%%\n\n", level.getSeniorProbability() * 100));
        
        // Key factors
        if (!level.getKeyFactors().isEmpty()) {
            narrative.append("### Key Factors\n");
            for (RoleInference.LevelFactor factor : level.getKeyFactors()) {
                narrative.append(String.format("- **%s**: %s\n", factor.getFactorName(), factor.getDescription()));
            }
            narrative.append("\n");
        }
        
        // Top 3 closest roles
        narrative.append("### Closest Role Targets\n");
        List<RoleInference.RoleDistance> topRoles = inference.getRoleDistances().stream()
            .limit(3)
            .toList();
        
        for (RoleInference.RoleDistance role : topRoles) {
            narrative.append(String.format("\n**%s** (Distance: %.1f)\n", role.getRoleName(), role.getOverallDistance()));
            narrative.append(String.format("- Skills matched: %d/%d critical, %d/%d important\n",
                role.getCriticalSkillsMatched(), role.getCriticalSkillsTotal(),
                role.getImportantSkillsMatched(), role.getImportantSkillsTotal()));
            narrative.append(String.format("- Estimated time: %d weeks\n", role.getEstimatedWeeksToReach()));
            narrative.append(String.format("- Difficulty: %s\n", role.getDifficultyAssessment()));
            
            if (!role.getTopGaps().isEmpty()) {
                narrative.append("- Top gaps: ").append(String.join(", ", role.getTopGaps())).append("\n");
            }
            
            narrative.append("- ").append(role.getMarketInsight()).append("\n");
        }
        
        return narrative.toString();
    }
    
    // ============================================
    // MARKET-AWARE RECOMMENDATION ENHANCEMENT
    // ============================================
    
    /**
     * Enhance recommendations with market context and adjusted priorities.
     */
    public List<ScoredRecommendation> enhanceWithMarketContext(List<ScoredRecommendation> recommendations) {
        log.info("Enhancing {} recommendations with market context", recommendations.size());
        
        List<ScoredRecommendation> enhanced = new ArrayList<>();
        
        for (ScoredRecommendation rec : recommendations) {
            // Get market-adjusted score
            double adjustedScore = marketDemandService.computeMarketAdjustedScore(
                rec.getSkill().toLowerCase().replace(" ", "-"), 
                rec.getTotalScore()
            );
            
            // Get market insight
            String marketInsight = marketDemandService.getMarketInsight(
                rec.getSkill().toLowerCase().replace(" ", "-")
            );
            
            // Create enhanced recommendation with market data
            Map<String, Integer> enhancedBreakdown = new HashMap<>(rec.getScoreBreakdown());
            enhancedBreakdown.put("marketAdjustment", (int) (adjustedScore - rec.getTotalScore()));
            
            String enhancedReason = rec.getReason() + " " + marketInsight;
            
            enhanced.add(ScoredRecommendation.builder()
                .skill(rec.getSkill())
                .category(rec.getCategory())
                .priority(rec.getPriority())
                .totalScore((int) adjustedScore)
                .scoreBreakdown(enhancedBreakdown)
                .reason(enhancedReason)
                .prerequisites(rec.getPrerequisites())
                .unlocks(rec.getUnlocks())
                .difficultyLevel(rec.getDifficultyLevel())
                .estimatedTime(rec.getEstimatedTime())
                .careerImpact(enhanceCareerImpact(rec.getCareerImpact(), rec.getSkill()))
                .resources(rec.getResources())
                .evidenceBasis(rec.getEvidenceBasis())
                .build());
        }
        
        // Re-sort by adjusted score
        enhanced.sort((a, b) -> Integer.compare(b.getTotalScore(), a.getTotalScore()));
        
        // Re-assign priorities
        for (int i = 0; i < enhanced.size(); i++) {
            enhanced.get(i).setPriority(i + 1);
        }
        
        return enhanced;
    }
    
    private String enhanceCareerImpact(String baseImpact, String skill) {
        double demandScore = marketDemandService.getDemandScore(skill.toLowerCase().replace(" ", "-"));
        double growthRate = marketDemandService.getGrowthRate(skill.toLowerCase().replace(" ", "-"));
        
        if (demandScore >= 90 && growthRate > 15) {
            return "very_high";
        } else if (demandScore >= 80 || growthRate > 20) {
            return "high";
        } else if (demandScore >= 60) {
            return baseImpact;
        }
        return baseImpact;
    }
    
    /**
     * Generate a complete "why it matters now" explanation for a skill.
     */
    public String explainWhySkillMattersNow(String skillId, String userId) {
        StringBuilder explanation = new StringBuilder();
        
        // Market context
        Optional<MarketDemand> demandOpt = marketDemandService.getDemand(skillId);
        if (demandOpt.isPresent()) {
            MarketDemand demand = demandOpt.get();
            explanation.append("### Why ").append(demand.getSkillName()).append(" Matters Now\n\n");
            
            // Current demand
            explanation.append("**Market Demand:** ")
                .append(demand.getCurrentDemand().getDemandLevel().replace("_", " "))
                .append(" (score: ").append((int) demand.getCurrentDemand().getDemandScore()).append("/100)\n\n");
            
            // Trend
            if (demand.getTrend().getDirection().equals("rising")) {
                explanation.append("📈 **Trend:** Rising at ")
                    .append(String.format("%.0f%%", demand.getCurrentDemand().getGrowthRate()))
                    .append(" per year\n\n");
            } else if (demand.getTrend().getDirection().equals("declining")) {
                explanation.append("📉 **Trend:** Declining - consider if this aligns with your goals\n\n");
            } else {
                explanation.append("➡️ **Trend:** Stable demand\n\n");
            }
            
            // Salary impact
            if (demand.getCurrentDemand().getSalaryMultiplier() > 1.0) {
                explanation.append("💰 **Salary Premium:** ")
                    .append(String.format("%.0f%%", (demand.getCurrentDemand().getSalaryMultiplier() - 1) * 100))
                    .append(" above average\n\n");
            }
        }
        
        // Distance to goal context
        Optional<RoleInference> inferenceOpt = getLatestInference(userId);
        if (inferenceOpt.isPresent()) {
            RoleInference inference = inferenceOpt.get();
            
            // Check if this skill appears in any role gaps
            for (RoleInference.RoleDistance role : inference.getRoleDistances()) {
                boolean isGap = role.getTopGaps().stream()
                    .anyMatch(gap -> gap.toLowerCase().contains(skillId.toLowerCase()));
                
                if (isGap) {
                    explanation.append("🎯 **Role Gap:** This skill is needed for ")
                        .append(role.getRoleName())
                        .append(" (currently ").append(String.format("%.0f%%", 100 - role.getOverallDistance()))
                        .append(" there)\n\n");
                    break;
                }
            }
        }
        
        // Progression context
        Optional<SkillProgression> progressionOpt = getLatestProgression(userId);
        if (progressionOpt.isPresent()) {
            SkillProgression progression = progressionOpt.get();
            
            // Check if this is an area needing attention
            if (progression.getTrends().getAreasNeedingAttention().contains(skillId)) {
                explanation.append("⚠️ **Your Progress:** This area needs attention based on recent trends\n\n");
            }
        }
        
        return explanation.toString();
    }
    
    /**
     * Generate a complete intelligence report combining all insights.
     */
    public String generateIntelligenceReport(String userId) {
        StringBuilder report = new StringBuilder();
        
        report.append("# GitUpskill Intelligence Report\n\n");
        report.append("---\n\n");
        
        // Role analysis
        report.append(generateRoleNarrative(userId)).append("\n---\n\n");
        
        // Progression analysis
        report.append(generateProgressionNarrative(userId)).append("\n---\n\n");
        
        // Market context
        report.append("## Market Insights\n\n");
        
        List<MarketDemand> risingSkills = marketDemandService.getRisingSkills(5);
        if (!risingSkills.isEmpty()) {
            report.append("### Fastest Growing Skills\n");
            for (MarketDemand skill : risingSkills) {
                report.append(String.format("- **%s**: +%.0f%% growth, demand score %d/100\n",
                    skill.getSkillName(),
                    skill.getCurrentDemand().getGrowthRate(),
                    (int) skill.getCurrentDemand().getDemandScore()));
            }
            report.append("\n");
        }
        
        List<MarketDemand> topSkills = marketDemandService.getTopDemandSkills(5);
        if (!topSkills.isEmpty()) {
            report.append("### Highest Demand Skills\n");
            for (MarketDemand skill : topSkills) {
                report.append(String.format("- **%s**: %s demand, %.1fx salary multiplier\n",
                    skill.getSkillName(),
                    skill.getCurrentDemand().getDemandLevel().replace("_", " "),
                    skill.getCurrentDemand().getSalaryMultiplier()));
            }
        }
        
        return report.toString();
    }
}
