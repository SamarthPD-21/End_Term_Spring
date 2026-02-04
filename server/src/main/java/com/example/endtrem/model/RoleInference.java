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
 * Stores role inference results from the ML model.
 * Estimates user's current level and distance from target roles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "role_inferences")
public class RoleInference {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    @Indexed
    private String skillProfileId;
    
    // Inferred current level
    private InferredLevel inferredLevel;
    
    // Role distance calculations
    private List<RoleDistance> roleDistances;
    
    // Feature vector used for inference (for explainability)
    private FeatureVector features;
    
    // Model metadata
    private ModelMetadata modelInfo;
    
    @CreatedDate
    private Instant createdAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InferredLevel {
        private String level; // "junior", "mid", "senior"
        private double confidence; // 0.0 to 1.0
        
        // Probability distribution across levels
        private double juniorProbability;
        private double midProbability;
        private double seniorProbability;
        
        // Key factors that influenced the decision
        private List<LevelFactor> keyFactors;
        
        // Human-readable explanation
        private String explanation;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelFactor {
        private String factorName;
        private double value;
        private double weight;
        private double contribution; // How much this factor influenced the decision
        private String description;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleDistance {
        private String roleId;
        private String roleName;
        private String roleLevel; // junior/mid/senior
        private String specialization; // backend/frontend/fullstack/devops/data
        
        // Distance metrics (0 = perfect match, 100 = very far)
        private double overallDistance;
        private double technicalDistance;
        private double experienceDistance;
        private double engineeringPracticesDistance;
        
        // Breakdown of what's needed
        private int criticalSkillsMatched;
        private int criticalSkillsTotal;
        private int importantSkillsMatched;
        private int importantSkillsTotal;
        
        // Time estimates
        private int estimatedWeeksToReach;
        private String difficultyAssessment; // "achievable", "challenging", "stretch goal"
        
        // Gap summary
        private List<String> topGaps;
        private List<String> strengths;
        
        // Market context
        private double marketDemandScore;
        private String marketInsight;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureVector {
        // Skill count features
        private int totalSkillCount;
        private int advancedSkillCount; // proficiency > 70
        private int intermediateSkillCount; // proficiency 40-70
        private int beginnerSkillCount; // proficiency < 40
        
        // Category coverage
        private int languageCount;
        private int frameworkCount;
        private int databaseCount;
        private int infrastructureCount;
        private int conceptCount;
        
        // Proficiency aggregates
        private double averageProficiency;
        private double maxProficiency;
        private double proficiencyStdDev;
        
        // Evidence strength
        private double averageEvidenceStrength;
        private int totalProjectCount;
        
        // Engineering maturity
        private double testingScore;
        private double documentationScore;
        private double ciCdScore;
        
        // Depth vs breadth
        private double specializationScore; // High = deep in few areas
        private double breadthScore; // High = wide coverage
        
        // Computed composite features
        private double technicalMaturityScore;
        private double experienceIndicator;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelMetadata {
        private String modelVersion;
        private String modelType; // "logistic_regression"
        private Instant trainedAt;
        private double modelAccuracy;
        private Map<String, Double> featureImportance;
    }
}
