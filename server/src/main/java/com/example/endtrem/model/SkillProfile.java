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
 * Persisted skill profile inferred from README analysis.
 * This is the deterministic output of skill extraction before recommendation generation.
 * Stored for reproducibility, testing, and historical tracking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "skill_profiles")
public class SkillProfile {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    // Version for tracking changes over time
    private int version;
    
    // ===== DETECTED SKILLS =====
    private List<DetectedSkill> detectedSkills;
    
    // ===== SKILL CATEGORIES =====
    private Map<String, List<DetectedSkill>> skillsByCategory;
    
    // ===== ENGINEERING DISCIPLINE SCORES =====
    private EngineeringScores engineeringScores;
    
    // ===== METADATA =====
    private int totalRepositoriesAnalyzed;
    private List<String> analyzedRepositoryIds;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    /**
     * A skill detected from repository analysis with confidence scoring
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectedSkill {
        private String name;
        private String normalizedName; // Canonical name from ontology
        private String category; // language, framework, database, tool, concept, methodology
        private String subcategory; // e.g., frontend, backend, devops
        
        // Proficiency scoring
        private int proficiencyScore; // 1-100
        private int projectCount; // Number of projects using this skill
        private int evidenceStrength; // 1-100, how confident we are
        
        // Evidence sources
        private List<String> evidenceSources; // Repo names where detected
        private String primaryEvidence; // Main evidence description
        
        // Trend analysis
        private String trend; // improving, stable, declining
        private int recentProjectCount; // Projects in last 6 months
    }
    
    /**
     * Engineering discipline scores derived from project analysis
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngineeringScores {
        private int testingScore; // 0-100
        private int documentationScore;
        private int ciCdScore;
        private int codeStructureScore;
        private int securityAwarenessScore;
        
        // Ratios
        private double testingCoverage; // % of repos with tests
        private double ciCdCoverage;
        private double docCoverage;
        
        // Detailed breakdowns
        private Map<String, Integer> testingDetails;
        private Map<String, Integer> documentationDetails;
    }
}
