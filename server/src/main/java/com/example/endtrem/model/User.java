package com.example.endtrem.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String email;
    
    private String password; // Encrypted, null for OAuth users
    
    private String name;
    
    @Indexed(unique = true, sparse = true)
    private String githubUsername;
    
    private String githubId;
    
    private String githubAccessToken; // Encrypted
    
    private String avatarUrl;
    
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;
    
    @Builder.Default
    private Set<String> roles = new HashSet<>(Set.of("ROLE_USER"));
    
    @Builder.Default
    private boolean enabled = true;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastAnalysisAt;
    
    public enum AuthProvider {
        LOCAL, GITHUB
    }
}
