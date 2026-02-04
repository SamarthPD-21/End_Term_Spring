package com.example.endtrem.repository;

import com.example.endtrem.model.MarketDemand;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketDemandRepository extends MongoRepository<MarketDemand, String> {
    
    Optional<MarketDemand> findBySkillId(String skillId);
    
    List<MarketDemand> findByCategory(String category);
    
    List<MarketDemand> findByCurrentDemandDemandLevelIn(List<String> levels);
    
    List<MarketDemand> findTop20ByOrderByCurrentDemandDemandScoreDesc();
}
