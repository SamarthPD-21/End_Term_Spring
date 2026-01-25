package com.example.endtrem.service;

import com.example.endtrem.config.GitHubConfig;
import com.example.endtrem.dto.RepositoryDTO;
import com.example.endtrem.model.Repository;
import com.example.endtrem.model.User;
import com.example.endtrem.repository.RepositoryRepository;
import com.example.endtrem.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
    
    public String getGitHubLoginUrl() {
        return String.format(
            "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=%s&state=%s",
            gitHubConfig.getClientId(),
            gitHubConfig.getRedirectUri(),
            gitHubConfig.getScope().replace(" ", "%20"),
            UUID.randomUUID().toString()
        );
    }
    
    public Map<String, String> exchangeCodeForToken(String code) {
        WebClient client = webClientBuilder.baseUrl("https://github.com").build();
        
        JsonNode response = client.post()
            .uri("/login/oauth/access_token")
            .header("Accept", "application/json")
            .bodyValue(Map.of(
                "client_id", gitHubConfig.getClientId(),
                "client_secret", gitHubConfig.getClientSecret(),
                "code", code,
                "redirect_uri", gitHubConfig.getRedirectUri()
            ))
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
        
        if (response == null || response.has("error")) {
            throw new RuntimeException("Failed to exchange code for token: " + 
                (response != null ? response.get("error_description").asText() : "Unknown error"));
        }
        
        return Map.of("access_token", response.get("access_token").asText());
    }
    
    public Map<String, Object> getGitHubUserInfo(String accessToken) {
        JsonNode userInfo = gitHubWebClient.get()
            .uri("/user")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
        
        // Get email if not public
        String email = userInfo.has("email") && !userInfo.get("email").isNull() 
            ? userInfo.get("email").asText() 
            : fetchPrimaryEmail(accessToken);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", userInfo.get("id").asText());
        result.put("login", userInfo.get("login").asText());
        result.put("name", userInfo.has("name") && !userInfo.get("name").isNull() 
            ? userInfo.get("name").asText() 
            : userInfo.get("login").asText());
        result.put("email", email);
        result.put("avatar_url", userInfo.get("avatar_url").asText());
        
        return result;
    }
    
    private String fetchPrimaryEmail(String accessToken) {
        JsonNode emails = gitHubWebClient.get()
            .uri("/user/emails")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
        
        if (emails != null && emails.isArray()) {
            for (JsonNode email : emails) {
                if (email.get("primary").asBoolean()) {
                    return email.get("email").asText();
                }
            }
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
        
        List<JsonNode> allRepos = new ArrayList<>();
        int page = 1;
        final int perPage = 100;
        
        while (true) {
            final int currentPage = page;
            JsonNode repos = gitHubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/user/repos")
                    .queryParam("visibility", "public")
                    .queryParam("sort", "updated")
                    .queryParam("per_page", perPage)
                    .queryParam("page", currentPage)
                    .build())
                .header("Authorization", "Bearer " + user.getGithubAccessToken())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (repos == null || !repos.isArray() || repos.size() == 0) {
                break;
            }
            
            for (JsonNode repo : repos) {
                allRepos.add(repo);
            }
            
            if (repos.size() < perPage) {
                break;
            }
            page++;
        }
        
        List<Repository> savedRepos = new ArrayList<>();
        
        for (JsonNode repoNode : allRepos) {
            LocalDateTime updatedAt = parseGitHubDate(repoNode.get("updated_at").asText());
            boolean isFork = repoNode.get("fork").asBoolean();
            
            // Filter by date and fork status
            if (updatedAt.isBefore(sinceDate)) continue;
            if (isFork && !includeForked) continue;
            
            Long githubRepoId = repoNode.get("id").asLong();
            
            Repository repo = repositoryRepository.findByUserIdAndGithubRepoId(userId, githubRepoId)
                .orElse(new Repository());
            
            repo.setUserId(userId);
            repo.setGithubRepoId(githubRepoId);
            repo.setName(repoNode.get("name").asText());
            repo.setFullName(repoNode.get("full_name").asText());
            repo.setDescription(repoNode.has("description") && !repoNode.get("description").isNull() 
                ? repoNode.get("description").asText() : "");
            repo.setHtmlUrl(repoNode.get("html_url").asText());
            repo.setLanguage(repoNode.has("language") && !repoNode.get("language").isNull() 
                ? repoNode.get("language").asText() : null);
            repo.setStargazersCount(repoNode.get("stargazers_count").asInt());
            repo.setForksCount(repoNode.get("forks_count").asInt());
            repo.setSize(repoNode.get("size").asInt());
            repo.setPrivate(repoNode.get("private").asBoolean());
            repo.setFork(isFork);
            repo.setDefaultBranch(repoNode.get("default_branch").asText());
            repo.setGithubCreatedAt(parseGitHubDate(repoNode.get("created_at").asText()));
            repo.setGithubUpdatedAt(updatedAt);
            repo.setGithubPushedAt(parseGitHubDate(repoNode.get("pushed_at").asText()));
            
            // Parse topics
            if (repoNode.has("topics") && repoNode.get("topics").isArray()) {
                List<String> topics = new ArrayList<>();
                for (JsonNode topic : repoNode.get("topics")) {
                    topics.add(topic.asText());
                }
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
            JsonNode response = gitHubWebClient.get()
                .uri("/repos/" + fullName + "/readme")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github.v3.raw")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (response != null && response.has("content")) {
                String content = response.get("content").asText();
                // Decode base64
                return new String(Base64.getMimeDecoder().decode(content));
            }
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
            .name(repo.getName())
            .fullName(repo.getFullName())
            .description(repo.getDescription())
            .htmlUrl(repo.getHtmlUrl())
            .language(repo.getLanguage())
            .topics(repo.getTopics())
            .stars(repo.getStargazersCount())
            .forks(repo.getForksCount())
            .processed(repo.isProcessed())
            .updatedAt(repo.getGithubUpdatedAt())
            .build();
    }
}
