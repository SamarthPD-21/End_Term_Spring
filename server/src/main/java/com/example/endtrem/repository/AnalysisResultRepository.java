package com.example.endtrem.repository;

import com.example.endtrem.model.AnalysisResult;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends MongoRepository<AnalysisResult, String> {
    
    List<AnalysisResult> findByUserId(String userId, Sort sort);
    
    Optional<AnalysisResult> findFirstByUserIdOrderByCreatedAtDesc(String userId);
    
    List<AnalysisResult> findTop10ByUserIdOrderByCreatedAtDesc(String userId);
    
    void deleteByUserId(String userId);
    
    long countByUserId(String userId);
}
