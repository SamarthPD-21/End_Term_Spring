package com.example.endtrem.repository;

import com.example.endtrem.model.PromptTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromptTemplateRepository extends MongoRepository<PromptTemplate, String> {
    
    Optional<PromptTemplate> findByName(String name);
    
    Optional<PromptTemplate> findByNameAndActive(String name, boolean active);
    
    Optional<PromptTemplate> findByTypeAndActive(PromptTemplate.PromptType type, boolean active);
}
