package com.example.endtrem.controller;

import com.example.endtrem.dto.ApiResponse;
import com.example.endtrem.dto.AuthResponse;
import com.example.endtrem.dto.GitHubCallbackRequest;
import com.example.endtrem.dto.RepositoryDTO;
import com.example.endtrem.security.UserPrincipal;
import com.example.endtrem.service.AuthService;
import com.example.endtrem.service.GitHubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GitHubController {
    
    private final GitHubService gitHubService;
    private final AuthService authService;
    
    /**
     * Get GitHub OAuth login URL
     */
    @GetMapping("/login-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getLoginUrl() {
        String url = gitHubService.getGitHubLoginUrl();
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }
    
    /**
     * Handle GitHub OAuth callback
     */
    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<AuthResponse>> handleCallback(@RequestBody GitHubCallbackRequest request) {
        try {
            // Exchange code for access token
            Map<String, String> tokenResponse = gitHubService.exchangeCodeForToken(request.getCode());
            String accessToken = tokenResponse.get("access_token");
            
            // Get user info from GitHub
            Map<String, Object> userInfo = gitHubService.getGitHubUserInfo(accessToken);
            
            // Create or update user in our system
            AuthResponse response = authService.processGitHubUser(
                userInfo.get("id").toString(),
                userInfo.get("email").toString(),
                userInfo.get("name").toString(),
                userInfo.get("login").toString(),
                userInfo.get("avatar_url").toString(),
                accessToken
            );
            
            return ResponseEntity.ok(ApiResponse.success("GitHub login successful", response));
        } catch (Exception e) {
            log.error("GitHub callback error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("GitHub authentication failed: " + e.getMessage()));
        }
    }
    
    /**
     * Fetch repositories for the current user
     */
    @PostMapping("/fetch-repos")
    public ResponseEntity<ApiResponse<List<RepositoryDTO>>> fetchRepositories(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(defaultValue = "false") boolean includeForked) {
        try {
            List<RepositoryDTO> repos = gitHubService.fetchAndSaveRepositories(
                principal.getId(), months, includeForked);
            return ResponseEntity.ok(ApiResponse.success("Fetched " + repos.size() + " repositories", repos));
        } catch (Exception e) {
            log.error("Failed to fetch repos: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Get cached repositories for the current user
     */
    @GetMapping("/repos")
    public ResponseEntity<ApiResponse<List<RepositoryDTO>>> getRepositories(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            List<RepositoryDTO> repos = gitHubService.getUserRepositories(principal.getId());
            return ResponseEntity.ok(ApiResponse.success(repos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
