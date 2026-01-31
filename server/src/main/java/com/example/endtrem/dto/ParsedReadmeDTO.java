package com.example.endtrem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO containing all parsed README data extracted without AI
 * This is the output of the non-AI small pipeline
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedReadmeDTO {
    
    private String repoName;
    private String repoId;
    private String description;
    private String projectType;
    private ComplexityLevel complexity;
    private boolean hasReadme;
    private int readmeLength;
    
    // Structured extracted data
    private TechStack techStack;
    private ProjectMaturity projectMaturity;
    private EngineeringDiscipline engineeringDiscipline;
    private VibeCodingAnalysis vibeCodingAnalysis;
    
    /**
     * Tech Stack & Tools extracted from README
     * Very reliable - based on keywords, badges, setup instructions
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechStack {
        private List<String> languages;
        private List<String> frameworks;
        private List<String> databases;
        private List<String> infrastructure;
        private List<String> authentication;
        private List<String> tools;
        
        public int getTotalTechnologies() {
            return (languages != null ? languages.size() : 0) +
                   (frameworks != null ? frameworks.size() : 0) +
                   (databases != null ? databases.size() : 0) +
                   (infrastructure != null ? infrastructure.size() : 0) +
                   (authentication != null ? authentication.size() : 0) +
                   (tools != null ? tools.size() : 0);
        }
    }
    
    /**
     * Project Scope & Maturity signals
     * Moderately to highly reliable
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectMaturity {
        // Features detected
        private List<String> featuresImplemented;
        private List<String> apiEndpoints;
        private List<String> engineeringPatterns;
        
        // Documentation quality
        private boolean hasArchitectureDiagram;
        private boolean hasScreenshots;
        private boolean hasApiDocumentation;
        private boolean hasDemoLink;
        
        // Overall score
        private int featureDepthScore;
    }
    
    /**
     * Engineering Discipline signals
     * Strong indirect signal of coding quality
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngineeringDiscipline {
        // Setup quality
        private boolean hasInstallationSteps;
        private boolean hasEnvironmentVariables;
        private boolean hasEnvExample;
        
        // Best practices
        private boolean hasContributing;
        private boolean hasLicense;
        private boolean hasTests;
        private boolean hasCiCd;
        
        // Documentation structure
        private List<String> documentationSections;
        private List<String> qualityIndicators;
        
        // Overall score
        private int setupQualityScore;
    }
    
    /**
     * Vibe Coding Analysis
     * Heuristic-based probability estimation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VibeCodingAnalysis {
        // Score from 0-100 (higher = more "vibe coding" patterns)
        private int vibeScore;
        
        // Human-friendly label
        private String codingStyle;
        
        // Signals that suggest rapid prototyping / vibe coding
        private List<String> vibeSignals;
        
        // Signals that suggest disciplined / structured coding
        private List<String> disciplinedSignals;
    }
    
    public enum ComplexityLevel {
        BEGINNER, INTERMEDIATE, ADVANCED
    }
}
