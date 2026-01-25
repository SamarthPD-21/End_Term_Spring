package com.example.endtrem.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "repo_summaries")
public class RepoSummary {
    
    @Id
    private String id;
    
    @Indexed
    private String repositoryId;
    
    @Indexed
    private String userId;
    
    private String repoName;
    
    private List<String> technologies;
    
    private List<String> frameworks;
    
    private List<String> features;
    
    private String projectType;
    
    private ComplexityLevel complexity;
    
    private String description;
    
    private List<String> keywords;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    public enum ComplexityLevel {
        BEGINNER, INTERMEDIATE, ADVANCED
    }
}
