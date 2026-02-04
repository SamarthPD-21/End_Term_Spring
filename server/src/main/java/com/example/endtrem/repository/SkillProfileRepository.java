package com.example.endtrem.repository;

import com.example.endtrem.model.SkillProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SkillProfile persistence
 */
@Repository
public interface SkillProfileRepository extends MongoRepository<SkillProfile, String> {
    
    /**
     * Find the latest skill profile for a user
     */
    Optional<SkillProfile> findFirstByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * Find all skill profiles for a user (history)
     */
    List<SkillProfile> findByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * Find the top N skill profiles for a user
     */
    List<SkillProfile> findTop10ByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * Count profiles for a user
     */
    long countByUserId(String userId);
    
    /**
     * Delete all profiles for a user
     */
    void deleteByUserId(String userId);
}
