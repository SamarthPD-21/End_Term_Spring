package com.example.endtrem.dto;

import com.example.endtrem.model.AnalysisResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    private String analysisId;
    private AnalysisResult.SkillAnalysis skillAnalysis;
    private List<AnalysisResult.LearningRecommendation> recommendations;
    private AnalysisResult.EnhancedRecommendations enhancedRecommendations;
    private AnalysisResult.DeveloperProfile developerProfile;
    private AnalysisResult.PipelineSummary pipelineSummary;
    private int repositoriesAnalyzed;
    private LocalDateTime analyzedAt;
}
