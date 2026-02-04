package com.example.endtrem.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Persisted recommendation results from the deterministic recommendation engine.
 * Contains both the structured recommendations and the LLM-formatted explanation.
 * Stored for reproducibility, testing, and historical tracking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "recommendation_results")
public class RecommendationResult {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    @Indexed
    private String skillProfileId; // Reference to the skill profile used
    
    @Indexed
    private String targetRoleId; // Role template used for comparison
    
    // ===== GAP ANALYSIS =====
    private GapAnalysis gapAnalysis;
    
    // ===== STRUCTURED RECOMMENDATIONS =====
    private List<ScoredRecommendation> recommendations;
    
    // ===== CATEGORIZED RECOMMENDATIONS =====
    private CategorizedRecommendations categorizedRecommendations;
    
    // ===== ENGINEERING HABIT RECOMMENDATIONS =====
    private List<EngineeringHabitRecommendation> engineeringHabits;
    
    // ===== LLM-FORMATTED OUTPUT (for display only) =====
    private FormattedOutput formattedOutput;
    
    // ===== SCORING METADATA =====
    private ScoringMetadata scoringMetadata;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    /**
     * Gap analysis comparing current skills to target role
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GapAnalysis {
        private String targetRole;
        private int overallReadiness; // 0-100 percentage
        
        private List<SkillGap> criticalGaps; // Must-have skills missing
        private List<SkillGap> importantGaps; // Important but not critical
        private List<SkillGap> niceToHaveGaps; // Optional enhancements
        
        private List<SkillMatch> strongMatches; // Skills that exceed requirements
        private List<SkillMatch> adequateMatches; // Skills that meet requirements
        private List<SkillMatch> partialMatches; // Skills that partially meet requirements
    }
    
    /**
     * A skill gap identified during analysis
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillGap {
        private String skillName;
        private String category;
        private int requiredLevel; // 1-100
        private int currentLevel; // 0 if missing, otherwise current proficiency
        private int gapSize; // requiredLevel - currentLevel
        
        private String importance; // CRITICAL, IMPORTANT, NICE_TO_HAVE
        private List<String> dependencyChain; // Skills needed before this one
        private String rationale; // Why this skill is needed for the role
    }
    
    /**
     * A skill that matches role requirements
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillMatch {
        private String skillName;
        private String category;
        private int requiredLevel;
        private int currentLevel;
        private int surplus; // currentLevel - requiredLevel (positive if exceeds)
        
        private String matchQuality; // STRONG, ADEQUATE, PARTIAL
    }
    
    /**
     * A scored recommendation with full explainability
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoredRecommendation {
        private String skill;
        private String category; // learn_new, improve_existing, practice_more
        private int priority; // 1-10, lower is higher priority
        
        // Scoring breakdown
        private int totalScore; // Composite score
        private Map<String, Integer> scoreBreakdown; // Component scores
        
        // Explainability
        private String reason;
        private List<String> prerequisites; // Skills to learn first
        private List<String> unlocks; // Skills this enables
        private String difficultyLevel; // beginner, intermediate, advanced
        private String estimatedTime;
        private String careerImpact; // high, medium, low
        
        // Learning resources
        private List<String> resources;
        
        // Evidence
        private String evidenceBasis; // Why this recommendation was made
    }
    
    /**
     * Recommendations categorized by type
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorizedRecommendations {
        private List<ScoredRecommendation> skillsToLearn;
        private List<ScoredRecommendation> skillsToImprove;
        private List<ScoredRecommendation> practiceMore;
    }
    
    /**
     * Engineering habit recommendation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngineeringHabitRecommendation {
        private String habit;
        private String currentState;
        private String targetState;
        private List<String> actionItems;
        private int priority;
        private int currentScore;
        private int targetScore;
    }
    
    /**
     * LLM-formatted output for user display
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormattedOutput {
        private String executiveSummary;
        private String learningRoadmap;
        private String careerAdvice;
        private String nextMilestone;
        private List<String> quickWins; // Easy wins to start with
        private String professionalFeedback;
    }
    
    /**
     * Metadata about the scoring process
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoringMetadata {
        private String scoringVersion; // Version of scoring algorithm
        private Map<String, Double> weights; // Weights used in scoring
        private int totalSkillsEvaluated;
        private int totalGapsIdentified;
        private int totalRecommendationsGenerated;
        private long processingTimeMs;
    }
}
