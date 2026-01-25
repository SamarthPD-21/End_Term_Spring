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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedProfileDTO {
    private String userId;
    private List<String> languages;
    private List<String> frameworks;
    private List<String> tools;
    private List<String> projectTypes;
    private List<String> features;
    private Map<String, Integer> languageDistribution;
    private Map<String, Integer> complexityDistribution;
    private int totalProjects;
    private int totalStars;
    private List<String> potentialGaps;
}
