package com.example.endtrem.repository;

import com.example.endtrem.model.RepoSummary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepoSummaryRepository extends MongoRepository<RepoSummary, String> {
    
    List<RepoSummary> findByUserId(String userId);
    
    Optional<RepoSummary> findByRepositoryId(String repositoryId);
    
    void deleteByUserId(String userId);
    
    void deleteByRepositoryId(String repositoryId);
    
    long countByUserId(String userId);
}
