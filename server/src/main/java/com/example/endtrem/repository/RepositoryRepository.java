package com.example.endtrem.repository;

import com.example.endtrem.model.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface RepositoryRepository extends MongoRepository<Repository, String> {
    
    List<Repository> findByUserId(String userId);
    
    List<Repository> findByUserIdAndProcessed(String userId, boolean processed);
    
    Optional<Repository> findByUserIdAndGithubRepoId(String userId, Long githubRepoId);
    
    List<Repository> findByUserIdAndGithubUpdatedAtAfter(String userId, LocalDateTime after);
    
    void deleteByUserId(String userId);
    
    long countByUserId(String userId);
}
