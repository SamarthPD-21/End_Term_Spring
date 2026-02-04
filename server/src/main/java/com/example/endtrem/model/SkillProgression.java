package com.example.endtrem.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Tracks skill progression over time by computing deltas between skill profiles.
 * Enables understanding of whether a user is improving, stagnating, or regressing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "skill_progressions")
public class SkillProgression {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    // Reference to the skill profiles being compared
    private String previousProfileId;
    private String currentProfileId;
    
    // Time range of comparison
    private Instant previousSnapshotDate;
    private Instant currentSnapshotDate;
    private long daysBetweenSnapshots;
    
    // Overall progression summary
    private ProgressionSummary summary;
    
    // Detailed skill-by-skill changes
    private List<SkillDelta> skillDeltas;
    
    // Engineering practice progression
    private EngineeringProgressionMetrics engineeringProgression;
    
    // Velocity metrics
    private VelocityMetrics velocity;
    
    // Trend analysis
    private TrendAnalysis trends;
    
    @CreatedDate
    private Instant createdAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressionSummary {
        private ProgressionStatus overallStatus; // IMPROVING, STAGNATING, REGRESSING
        private int totalSkillsImproved;
        private int totalSkillsStagnant;
        private int totalSkillsRegressed;
        private int newSkillsAcquired;
        private int skillsAbandoned;
        private double overallProgressScore; // -100 to +100
        private String narrativeSummary;
    }
    
    public enum ProgressionStatus {
        RAPIDLY_IMPROVING,  // Score > 30
        IMPROVING,          // Score 10-30
        SLIGHTLY_IMPROVING, // Score 1-10
        STAGNATING,         // Score -5 to 5
        SLIGHTLY_REGRESSING, // Score -5 to -15
        REGRESSING          // Score < -15
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillDelta {
        private String skillName;
        private String normalizedName;
        private String category;
        
        // Previous state (null if new skill)
        private Integer previousProficiency;
        private Integer previousEvidence;
        private Integer previousProjectCount;
        
        // Current state (null if skill abandoned)
        private Integer currentProficiency;
        private Integer currentEvidence;
        private Integer currentProjectCount;
        
        // Computed deltas
        private int proficiencyDelta;
        private int evidenceDelta;
        private int projectCountDelta;
        
        // Classification
        private DeltaType deltaType;
        private String insight; // Human-readable explanation
    }
    
    public enum DeltaType {
        NEW_SKILL,          // Skill didn't exist before
        SIGNIFICANT_GAIN,   // Proficiency increased > 15
        MODERATE_GAIN,      // Proficiency increased 5-15
        SLIGHT_GAIN,        // Proficiency increased 1-5
        STABLE,             // No change
        SLIGHT_DECLINE,     // Proficiency decreased 1-5
        MODERATE_DECLINE,   // Proficiency decreased 5-15
        SIGNIFICANT_DECLINE,// Proficiency decreased > 15
        ABANDONED           // Skill no longer detected
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngineeringProgressionMetrics {
        private double testingScoreDelta;
        private double documentationScoreDelta;
        private double ciCdScoreDelta;
        
        private String testingTrend;     // "improving", "stable", "declining"
        private String documentationTrend;
        private String ciCdTrend;
        
        private String engineeringMaturityInsight;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VelocityMetrics {
        // Skills per month metrics
        private double newSkillsPerMonth;
        private double skillImprovementsPerMonth;
        private double projectsPerMonth;
        
        // Acceleration (change in velocity)
        private double velocityTrend; // Positive = accelerating, Negative = decelerating
        
        // Time estimates based on velocity
        private int estimatedDaysToNextLevel;
        private String velocityAssessment; // "fast learner", "steady progress", "needs momentum"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendAnalysis {
        // Category-level trends
        private Map<String, CategoryTrend> categoryTrends;
        
        // Strongest areas of growth
        private List<String> strongestGrowthAreas;
        
        // Areas needing attention
        private List<String> areasNeedingAttention;
        
        // Pattern detection
        private List<String> detectedPatterns; // e.g., "Shifting from frontend to fullstack"
        
        // Predictions
        private List<String> projectedMilestones;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryTrend {
        private String category;
        private double averageDelta;
        private int skillsImproved;
        private int skillsDeclined;
        private String trendDirection; // "up", "stable", "down"
    }
}
