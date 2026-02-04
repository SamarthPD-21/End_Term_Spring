package com.example.endtrem.repository;

import com.example.endtrem.model.RecommendationResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RecommendationResult persistence
 */
@Repository
public interface RecommendationResultRepository extends MongoRepository<RecommendationResult, String> {
    
    /**
     * Find the latest recommendation result for a user
     */
    Optional<RecommendationResult> findFirstByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * Find all recommendation results for a user (history)
     */
    List<RecommendationResult> findByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * Find the top N recommendation results for a user
     */
    List<RecommendationResult> findTop10ByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * Find recommendations by skill profile ID
     */
    Optional<RecommendationResult> findBySkillProfileId(String skillProfileId);
    
    /**
     * Find recommendations by target role
     */
    List<RecommendationResult> findByUserIdAndTargetRoleId(String userId, String targetRoleId);
    
    /**
     * Count recommendations for a user
     */
    long countByUserId(String userId);
    
    /**
     * Delete all recommendations for a user
     */
    void deleteByUserId(String userId);
}
