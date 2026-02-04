package com.example.endtrem.repository;

import com.example.endtrem.model.SkillProgression;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SkillProgressionRepository extends MongoRepository<SkillProgression, String> {
    
    Optional<SkillProgression> findFirstByUserIdOrderByCreatedAtDesc(String userId);
    
    List<SkillProgression> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<SkillProgression> findTop10ByUserIdOrderByCreatedAtDesc(String userId);
    
    List<SkillProgression> findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(
        String userId, Instant start, Instant end);
    
    Optional<SkillProgression> findByPreviousProfileIdAndCurrentProfileId(
        String previousProfileId, String currentProfileId);
    
    long countByUserId(String userId);
    
    void deleteByUserId(String userId);
}
