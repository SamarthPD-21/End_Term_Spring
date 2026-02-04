package com.example.endtrem.repository;

import com.example.endtrem.model.RoleInference;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleInferenceRepository extends MongoRepository<RoleInference, String> {
    
    Optional<RoleInference> findFirstByUserIdOrderByCreatedAtDesc(String userId);
    
    Optional<RoleInference> findBySkillProfileId(String skillProfileId);
    
    List<RoleInference> findTop10ByUserIdOrderByCreatedAtDesc(String userId);
    
    List<RoleInference> findByUserIdOrderByCreatedAtDesc(String userId);
    
    void deleteByUserId(String userId);
}
