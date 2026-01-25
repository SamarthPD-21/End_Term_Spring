package com.example.endtrem.controller;

import com.example.endtrem.dto.AnalysisRequest;
import com.example.endtrem.dto.AnalysisResponse;
import com.example.endtrem.dto.ApiResponse;
import com.example.endtrem.security.UserPrincipal;
import com.example.endtrem.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {
    
    private final AnalysisService analysisService;
    
    /**
     * Run a new skill analysis
     */
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<AnalysisResponse>> runAnalysis(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AnalysisRequest request) {
        try {
            log.info("Starting analysis for user: {}", principal.getId());
            AnalysisResponse response = analysisService.runAnalysis(principal.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("Analysis completed successfully", response));
        } catch (Exception e) {
            log.error("Analysis failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Get the latest analysis result
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<AnalysisResponse>> getLatestAnalysis(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            AnalysisResponse response = analysisService.getLatestAnalysis(principal.getId());
            if (response == null) {
                return ResponseEntity.ok(ApiResponse.success("No analysis found. Run your first analysis!", null));
            }
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Get analysis history
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AnalysisResponse>>> getAnalysisHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            List<AnalysisResponse> history = analysisService.getAnalysisHistory(principal.getId());
            return ResponseEntity.ok(ApiResponse.success(history));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Get specific analysis by ID
     */
    @GetMapping("/{analysisId}")
    public ResponseEntity<ApiResponse<AnalysisResponse>> getAnalysisById(
            @PathVariable String analysisId) {
        try {
            AnalysisResponse response = analysisService.getAnalysisById(analysisId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
