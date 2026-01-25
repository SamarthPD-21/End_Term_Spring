package com.example.endtrem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {
    private int monthsToAnalyze; // How many months back to fetch repos
    private boolean includeForkedRepos;
    private List<String> excludeRepos; // Repo names to exclude
    
    // New filter options
    private FilterMode filterMode; // How to filter repositories
    private List<String> selectedRepoIds; // Specific repo IDs to analyze (when filterMode is SELECTED)
    
    public enum FilterMode {
        TIME_BASED,      // Filter only by time (original behavior)
        SELECTED_REPOS,  // Only analyze selected repositories
        COMBINED         // Filter by time AND require selection
    }
}
