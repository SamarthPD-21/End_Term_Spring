package com.example.endtrem.controller;

import com.example.endtrem.dto.ApiResponse;
import com.example.endtrem.model.AnalysisResult;
import com.example.endtrem.repository.AnalysisResultRepository;
import com.example.endtrem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/analysis-results")
@RequiredArgsConstructor
public class AnalysisResultController {

    private final AnalysisResultRepository analysisResultRepository;

    /**
     * Get all analysis results for current user
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AnalysisResult>>> getAllResults(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            List<AnalysisResult> results = analysisResultRepository.findByUserId(
                    principal.getId(), 
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            log.error("Failed to get analysis results: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get latest analysis result
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<AnalysisResult>> getLatestResult(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            return analysisResultRepository.findFirstByUserIdOrderByCreatedAtDesc(principal.getId())
                    .map(result -> ResponseEntity.ok(ApiResponse.success(result)))
                    .orElse(ResponseEntity.ok(ApiResponse.success("No analysis results found", null)));
        } catch (Exception e) {
            log.error("Failed to get latest result: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get analysis result by ID
     */
    @GetMapping("/{resultId}")
    public ResponseEntity<ApiResponse<AnalysisResult>> getResultById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String resultId) {
        try {
            AnalysisResult result = analysisResultRepository.findById(resultId)
                    .orElseThrow(() -> new RuntimeException("Analysis result not found"));
            
            // Verify ownership
            if (!result.getUserId().equals(principal.getId())) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Access denied"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Failed to get analysis result: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get top 10 recent results
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<AnalysisResult>>> getRecentResults(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            List<AnalysisResult> results = analysisResultRepository
                    .findTop10ByUserIdOrderByCreatedAtDesc(principal.getId());
            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            log.error("Failed to get recent results: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get analysis count
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getResultCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            long count = analysisResultRepository.countByUserId(principal.getId());
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("Failed to get result count: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete analysis result by ID
     */
    @DeleteMapping("/{resultId}")
    public ResponseEntity<ApiResponse<Void>> deleteResult(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String resultId) {
        try {
            AnalysisResult result = analysisResultRepository.findById(resultId)
                    .orElseThrow(() -> new RuntimeException("Analysis result not found"));
            
            // Verify ownership
            if (!result.getUserId().equals(principal.getId())) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Access denied"));
            }
            
            analysisResultRepository.deleteById(resultId);
            log.info("Analysis result deleted: {}", resultId);
            return ResponseEntity.ok(ApiResponse.success("Analysis result deleted", null));
        } catch (Exception e) {
            log.error("Failed to delete result: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete all analysis results for current user
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAllResults(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            analysisResultRepository.deleteByUserId(principal.getId());
            log.info("All analysis results deleted for user: {}", principal.getId());
            return ResponseEntity.ok(ApiResponse.success("All analysis results deleted", null));
        } catch (Exception e) {
            log.error("Failed to delete results: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
