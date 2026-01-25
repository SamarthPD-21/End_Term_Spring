package com.example.endtrem.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "repositories")
@CompoundIndex(name = "user_repo_idx", def = "{'userId': 1, 'githubRepoId': 1}", unique = true)
public class Repository {
    
    @Id
    private String id;
    
    private String userId;
    
    private Long githubRepoId;
    
    private String name;
    
    private String fullName;
    
    private String description;
    
    private String htmlUrl;
    
    private String language;
    
    private List<String> topics;
    
    private int stargazersCount;
    
    private int forksCount;
    
    private int size;
    
    private boolean isPrivate;
    
    private boolean isFork;
    
    private String defaultBranch;
    
    private String readmeContent;
    
    private LocalDateTime githubCreatedAt;
    
    private LocalDateTime githubUpdatedAt;
    
    private LocalDateTime githubPushedAt;
    
    @CreatedDate
    private LocalDateTime fetchedAt;
    
    private boolean processed;
}
