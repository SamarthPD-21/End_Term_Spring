package com.example.endtrem.service;

import com.example.endtrem.config.GitHubConfig;
import com.example.endtrem.dto.RepositoryDTO;
import com.example.endtrem.model.Repository;
import com.example.endtrem.model.User;
import com.example.endtrem.repository.RepositoryRepository;
import com.example.endtrem.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {
    
    private final WebClient gitHubWebClient;
    private final WebClient.Builder webClientBuilder;
    private final GitHubConfig gitHubConfig;
    private final UserRepository userRepository;
    private final RepositoryRepository repositoryRepository;
    private final ObjectMapper objectMapper;
    
    public String getGitHubLoginUrl() {
        return String.format(
            "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=%s&state=%s",
            gitHubConfig.getClientId(),
            gitHubConfig.getRedirectUri(),
            gitHubConfig.getScope().replace(" ", "%20"),
            UUID.randomUUID().toString()
        );
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, String> exchangeCodeForToken(String code) {
        WebClient client = webClientBuilder.baseUrl("https://github.com").build();
        
        try {
            String responseBody = client.post()
                .uri("/login/oauth/access_token")
                .header("Accept", "application/json")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                    "client_id", gitHubConfig.getClientId(),
                    "client_secret", gitHubConfig.getClientSecret(),
                    "code", code,
                    "redirect_uri", gitHubConfig.getRedirectUri()
                ))
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            log.debug("GitHub token exchange response: {}", responseBody);
            
            Map<String, Object> response = objectMapper.readValue(responseBody, 
                new TypeReference<Map<String, Object>>() {});
            
            if (response.containsKey("error")) {
                String errorDescription = response.get("error_description") != null 
                    ? response.get("error_description").toString() 
                    : response.get("error").toString();
                throw new RuntimeException("GitHub OAuth error: " + errorDescription);
            }
            
            String accessToken = (String) response.get("access_token");
            if (accessToken == null || accessToken.isEmpty()) {
                throw new RuntimeException("No access token received from GitHub");
            }
            
            return Map.of("access_token", accessToken);
        } catch (WebClientResponseException e) {
            log.error("GitHub API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("GitHub API error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to exchange code for token: {}", e.getMessage());
            throw new RuntimeException("Failed to exchange code for token: " + e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getGitHubUserInfo(String accessToken) {
        try {
            String responseBody = gitHubWebClient.get()
                .uri("/user")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            Map<String, Object> userInfo = objectMapper.readValue(responseBody, 
                new TypeReference<Map<String, Object>>() {});
            
            // Get email if not public
            Object emailObj = userInfo.get("email");
            String email = (emailObj != null) ? emailObj.toString() : fetchPrimaryEmail(accessToken);
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", userInfo.get("id").toString());
            result.put("login", userInfo.get("login").toString());
            
            Object nameObj = userInfo.get("name");
            result.put("name", nameObj != null ? nameObj.toString() : userInfo.get("login").toString());
            result.put("email", email);
            result.put("avatar_url", userInfo.get("avatar_url").toString());
            
            return result;
        } catch (Exception e) {
            log.error("Failed to get GitHub user info: {}", e.getMessage());
            throw new RuntimeException("Failed to get GitHub user info: " + e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    private String fetchPrimaryEmail(String accessToken) {
        try {
            String responseBody = gitHubWebClient.get()
                .uri("/user/emails")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            List<Map<String, Object>> emails = objectMapper.readValue(responseBody, 
                new TypeReference<List<Map<String, Object>>>() {});
            
            for (Map<String, Object> email : emails) {
                Boolean isPrimary = (Boolean) email.get("primary");
                if (isPrimary != null && isPrimary) {
                    return email.get("email").toString();
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch primary email: {}", e.getMessage());
        }
        throw new RuntimeException("Could not fetch user email from GitHub");
    }
    
    public List<RepositoryDTO> fetchAndSaveRepositories(String userId, int monthsBack, boolean includeForked) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getGithubAccessToken() == null) {
            throw new RuntimeException("GitHub not linked to this account");
        }
        
        LocalDateTime sinceDate = LocalDateTime.now().minusMonths(monthsBack);
        
        List<Map<String, Object>> allRepos = new ArrayList<>();
        int page = 1;
        final int perPage = 100;
        
        try {
            while (true) {
                final int currentPage = page;
                String responseBody = gitHubWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/user/repos")
                        .queryParam("visibility", "public")
                        .queryParam("sort", "updated")
                        .queryParam("per_page", perPage)
                        .queryParam("page", currentPage)
                        .build())
                    .header("Authorization", "Bearer " + user.getGithubAccessToken())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                List<Map<String, Object>> repos = objectMapper.readValue(responseBody, 
                    new TypeReference<List<Map<String, Object>>>() {});
                
                if (repos == null || repos.isEmpty()) {
                    break;
                }
                
                allRepos.addAll(repos);
                
                if (repos.size() < perPage) {
                    break;
                }
                page++;
            }
        } catch (Exception e) {
            log.error("Failed to fetch repositories: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch repositories: " + e.getMessage());
        }
        
        List<Repository> savedRepos = new ArrayList<>();
        
        for (Map<String, Object> repoNode : allRepos) {
            LocalDateTime updatedAt = parseGitHubDate(repoNode.get("updated_at").toString());
            boolean isFork = (Boolean) repoNode.get("fork");
            
            // Filter by date and fork status
            if (updatedAt.isBefore(sinceDate)) continue;
            if (isFork && !includeForked) continue;
            
            Long githubRepoId = ((Number) repoNode.get("id")).longValue();
            
            Repository repo = repositoryRepository.findByUserIdAndGithubRepoId(userId, githubRepoId)
                .orElse(new Repository());
            
            repo.setUserId(userId);
            repo.setGithubRepoId(githubRepoId);
            repo.setName(repoNode.get("name").toString());
            repo.setFullName(repoNode.get("full_name").toString());
            repo.setDescription(repoNode.get("description") != null ? repoNode.get("description").toString() : "");
            repo.setHtmlUrl(repoNode.get("html_url").toString());
            repo.setLanguage(repoNode.get("language") != null ? repoNode.get("language").toString() : null);
            repo.setStargazersCount(((Number) repoNode.get("stargazers_count")).intValue());
            repo.setForksCount(((Number) repoNode.get("forks_count")).intValue());
            repo.setSize(((Number) repoNode.get("size")).intValue());
            repo.setPrivate((Boolean) repoNode.get("private"));
            repo.setFork(isFork);
            repo.setDefaultBranch(repoNode.get("default_branch").toString());
            repo.setGithubCreatedAt(parseGitHubDate(repoNode.get("created_at").toString()));
            repo.setGithubUpdatedAt(updatedAt);
            repo.setGithubPushedAt(parseGitHubDate(repoNode.get("pushed_at").toString()));
            
            // Parse topics
            Object topicsObj = repoNode.get("topics");
            if (topicsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> topics = ((List<Object>) topicsObj).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
                repo.setTopics(topics);
            }
            
            // Fetch README
            String readme = fetchReadme(user.getGithubAccessToken(), repo.getFullName(), repo.getDefaultBranch());
            repo.setReadmeContent(readme);
            repo.setProcessed(false);
            
            savedRepos.add(repositoryRepository.save(repo));
        }
        
        log.info("Fetched and saved {} repositories for user {}", savedRepos.size(), userId);
        
        return savedRepos.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    private String fetchReadme(String accessToken, String fullName, String branch) {
        try {
            // Request raw README content directly
            String readme = gitHubWebClient.get()
                .uri("/repos/" + fullName + "/readme")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github.v3.raw")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            return readme;
        } catch (Exception e) {
            log.debug("No README found for {}: {}", fullName, e.getMessage());
        }
        return null;
    }
    
    public List<RepositoryDTO> getUserRepositories(String userId) {
        return repositoryRepository.findByUserId(userId).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    private LocalDateTime parseGitHubDate(String dateStr) {
        return ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
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
}
