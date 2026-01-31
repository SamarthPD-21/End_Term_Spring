package com.example.endtrem.controller;

import com.example.endtrem.dto.ApiResponse;
import com.example.endtrem.model.RepoSummary;
import com.example.endtrem.repository.RepoSummaryRepository;
import com.example.endtrem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/repo-summaries")
@RequiredArgsConstructor
public class RepoSummaryController {

    private final RepoSummaryRepository repoSummaryRepository;

    /**
     * Get all repo summaries for current user
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RepoSummary>>> getAllSummaries(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            List<RepoSummary> summaries = repoSummaryRepository.findByUserId(principal.getId());
            return ResponseEntity.ok(ApiResponse.success(summaries));
        } catch (Exception e) {
            log.error("Failed to get repo summaries: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get repo summary by ID
     */
    @GetMapping("/{summaryId}")
    public ResponseEntity<ApiResponse<RepoSummary>> getSummaryById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String summaryId) {
        try {
            RepoSummary summary = repoSummaryRepository.findById(summaryId)
                    .orElseThrow(() -> new RuntimeException("Repo summary not found"));
            
            // Verify ownership
            if (!summary.getUserId().equals(principal.getId())) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Access denied"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (Exception e) {
            log.error("Failed to get repo summary: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get repo summary by repository ID
     */
    @GetMapping("/by-repo/{repositoryId}")
    public ResponseEntity<ApiResponse<RepoSummary>> getSummaryByRepositoryId(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String repositoryId) {
        try {
            RepoSummary summary = repoSummaryRepository.findByRepositoryId(repositoryId)
                    .orElseThrow(() -> new RuntimeException("Repo summary not found"));
            
            // Verify ownership
            if (!summary.getUserId().equals(principal.getId())) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Access denied"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (Exception e) {
            log.error("Failed to get repo summary: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get summary count
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getSummaryCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            long count = repoSummaryRepository.countByUserId(principal.getId());
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("Failed to get summary count: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete repo summary by ID
     */
    @DeleteMapping("/{summaryId}")
    public ResponseEntity<ApiResponse<Void>> deleteSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String summaryId) {
        try {
            RepoSummary summary = repoSummaryRepository.findById(summaryId)
                    .orElseThrow(() -> new RuntimeException("Repo summary not found"));
            
            // Verify ownership
            if (!summary.getUserId().equals(principal.getId())) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Access denied"));
            }
            
            repoSummaryRepository.deleteById(summaryId);
            log.info("Repo summary deleted: {}", summaryId);
            return ResponseEntity.ok(ApiResponse.success("Repo summary deleted", null));
        } catch (Exception e) {
            log.error("Failed to delete summary: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete all repo summaries for current user
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAllSummaries(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            repoSummaryRepository.deleteByUserId(principal.getId());
            log.info("All repo summaries deleted for user: {}", principal.getId());
            return ResponseEntity.ok(ApiResponse.success("All repo summaries deleted", null));
        } catch (Exception e) {
            log.error("Failed to delete summaries: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
