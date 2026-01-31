package com.example.endtrem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Aggregated summary sent to AI for final analysis
 * This is the token-efficient representation of all user repositories
 * Enhanced with non-AI parsed README data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedProfileDTO {
    private String userId;
    
    // ===== TECH STACK (from non-AI parsing) =====
    private List<String> languages;
    private List<String> frameworks;
    private List<String> databases;
    private List<String> infrastructure;
    private List<String> authentication;
    private List<String> tools;
    
    // ===== PROJECT TYPES & FEATURES =====
    private List<String> projectTypes;
    private List<String> features;
    private List<String> engineeringPatterns;
    
    // ===== DISTRIBUTION STATS =====
    private Map<String, Integer> languageDistribution;
    private Map<String, Integer> complexityDistribution;
    private Map<String, Integer> projectTypeDistribution;
    
    // ===== ENGINEERING DISCIPLINE SIGNALS =====
    private EngineeringMetrics engineeringMetrics;
    
    // ===== VIBE CODING ANALYSIS =====
    private VibeCodingMetrics vibeCodingMetrics;
    
    // ===== TOTALS =====
    private int totalProjects;
    private int totalStars;
    private int reposWithReadme;
    private int avgReadmeLength;
    
    // ===== DETECTED GAPS =====
    private List<String> potentialGaps;
    
    // ===== PER-REPO SUMMARIES (for context) =====
    private List<RepoSnapshot> repoSnapshots;
    
    /**
     * Aggregated engineering discipline metrics across all repos
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngineeringMetrics {
        private int reposWithTests;
        private int reposWithCiCd;
        private int reposWithEnvExample;
        private int reposWithApiDocs;
        private int reposWithArchitectureDocs;
        private int reposWithContributing;
        private int avgSetupQualityScore;
        private int avgDocumentationSections;
        private List<String> commonQualityIndicators;
    }
    
    /**
     * Aggregated vibe coding analysis across all repos
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VibeCodingMetrics {
        private int avgVibeScore;
        private String overallCodingStyle;
        private List<String> commonVibeSignals;
        private List<String> commonDisciplinedSignals;
        private int rapidPrototypingRepos;
        private int structuredRepos;
    }
    
    /**
     * Brief snapshot of each repo for AI context
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepoSnapshot {
        private String name;
        private String projectType;
        private String complexity;
        private List<String> mainTechnologies;
        private List<String> keyFeatures;
        private int vibeScore;
        private String codingStyle;
        private boolean hasTests;
        private boolean hasCiCd;
    }
}
