package com.example.endtrem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String id;
    private String email;
    private String name;
    private String githubUsername;
    private String avatarUrl;
    private boolean hasGithubLinked;
    private LocalDateTime lastAnalysisAt;
    private LocalDateTime createdAt;
}
