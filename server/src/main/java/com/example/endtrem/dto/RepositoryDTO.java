package com.example.endtrem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryDTO {
    private String id;
    private String name;
    private String fullName;
    private String description;
    private String htmlUrl;
    private String language;
    private List<String> topics;
    private int stars;
    private int forks;
    private boolean processed;
    private LocalDateTime updatedAt;
}
