package com.example.endtrem.controller;

import com.example.endtrem.dto.ApiResponse;
import com.example.endtrem.dto.RepositoryDTO;
import com.example.endtrem.model.Repository;
import com.example.endtrem.repository.RepositoryRepository;
import com.example.endtrem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryRepository repositoryRepository;

    /**
     * Get all repositories for current user
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RepositoryDTO>>> getUserRepositories(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            List<RepositoryDTO> repos = repositoryRepository.findByUserId(principal.getId())
                    .stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(repos));
        } catch (Exception e) {
            log.error("Failed to get repositories: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get repository by ID
     */
    @GetMapping("/{repoId}")
    public ResponseEntity<ApiResponse<RepositoryDTO>> getRepositoryById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String repoId) {
        try {
            Repository repo = repositoryRepository.findById(repoId)
                    .orElseThrow(() -> new RuntimeException("Repository not found"));
            
            // Verify ownership
            if (!repo.getUserId().equals(principal.getId())) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Access denied"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(mapToDTO(repo)));
        } catch (Exception e) {
            log.error("Failed to get repository: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get processed repositories count
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<RepositoryStats>> getRepositoryStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            long total = repositoryRepository.countByUserId(principal.getId());
            List<Repository> repos = repositoryRepository.findByUserId(principal.getId());
            long processed = repos.stream().filter(Repository::isProcessed).count();
            
            RepositoryStats stats = new RepositoryStats(total, processed, total - processed);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to get repository stats: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete a repository record (not from GitHub)
     */
    @DeleteMapping("/{repoId}")
    public ResponseEntity<ApiResponse<Void>> deleteRepository(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String repoId) {
        try {
            Repository repo = repositoryRepository.findById(repoId)
                    .orElseThrow(() -> new RuntimeException("Repository not found"));
            
            // Verify ownership
            if (!repo.getUserId().equals(principal.getId())) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Access denied"));
            }
            
            repositoryRepository.deleteById(repoId);
            log.info("Repository deleted: {}", repo.getFullName());
            return ResponseEntity.ok(ApiResponse.success("Repository deleted", null));
        } catch (Exception e) {
            log.error("Failed to delete repository: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete all repositories for current user
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAllRepositories(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            repositoryRepository.deleteByUserId(principal.getId());
            log.info("All repositories deleted for user: {}", principal.getId());
            return ResponseEntity.ok(ApiResponse.success("All repositories deleted", null));
        } catch (Exception e) {
            log.error("Failed to delete repositories: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private RepositoryDTO mapToDTO(Repository repo) {
        return RepositoryDTO.builder()
                .id(repo.getId())
                .githubRepoId(repo.getGithubRepoId())
                .name(repo.getName())
                .fullName(repo.getFullName())
                .description(repo.getDescription())
                .htmlUrl(repo.getHtmlUrl())
                .language(repo.getLanguage())
                .topics(repo.getTopics())
                .stargazersCount(repo.getStargazersCount())
                .forksCount(repo.getForksCount())
                .isPrivate(repo.isPrivate())
                .isFork(repo.isFork())
                .defaultBranch(repo.getDefaultBranch())
                .hasReadme(repo.getReadmeContent() != null && !repo.getReadmeContent().isEmpty())
                .processed(repo.isProcessed())
                .githubCreatedAt(repo.getGithubCreatedAt())
                .githubUpdatedAt(repo.getGithubUpdatedAt())
                .build();
    }

    // Stats DTO
    public record RepositoryStats(long total, long processed, long pending) {}
}
