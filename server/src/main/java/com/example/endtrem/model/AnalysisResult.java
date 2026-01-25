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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "analysis_results")
public class AnalysisResult {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    private SkillAnalysis skillAnalysis;
    
    private List<LearningRecommendation> recommendations;
    
    private EnhancedRecommendations enhancedRecommendations;
    
    private DeveloperProfile developerProfile;
    
    private PipelineSummary pipelineSummary;
    
    private int totalRepositoriesAnalyzed;
    
    private int tokensUsed;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillAnalysis {
        private List<Skill> strongSkills;
        private List<Skill> moderateSkills;
        private List<Skill> weakSkills;
        private List<String> missingSkills;
        private Map<String, List<Skill>> skillsByCategory;
        private int totalSkillsCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Skill {
        private String name;
        private String category; // language, framework, tool, concept, methodology
        private int proficiencyScore; // 1-100
        private String evidence; // Why this skill was detected
        private int projectCount; // How many projects used this skill
        private String trend; // improving, stable, declining
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningRecommendation {
        private String skill;
        private String reason;
        private int priority; // 1-5
        private List<String> resources;
        private String estimatedTimeToLearn;
        private String category; // learn_new, improve_existing, practice_more
        private List<String> relatedSkills;
        private String difficultyLevel; // beginner, intermediate, advanced
        private String careerImpact; // high, medium, low
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnhancedRecommendations {
        private List<LearningRecommendation> skillsToLearn;
        private List<LearningRecommendation> skillsToImprove;
        private List<LearningRecommendation> practiceMore;
        private List<String> careerAdvice;
        private String nextMilestone;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeveloperProfile {
        private String experienceLevel; // Junior, Mid, Senior
        private List<String> primaryLanguages;
        private List<String> primaryFrameworks;
        private List<String> projectTypes;
        private String specialization;
        private Map<String, Integer> skillDistribution;
        private List<String> strengths;
        private List<String> areasForGrowth;
        private String careerStage; // entry, junior, mid, senior, lead
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipelineSummary {
        private int totalLanguages;
        private int totalFrameworks;
        private int totalTools;
        private long processingTimeMs;
        private List<RepoExtraction> repoExtractions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepoExtraction {
        private String repoId;
        private String repoName;
        private List<String> languages;
        private List<String> frameworks;
        private List<String> tools;
        private List<String> skills;
        private String complexity;
    }
}
