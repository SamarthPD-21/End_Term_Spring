package com.example.endtrem.repository;

import com.example.endtrem.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByGithubId(String githubId);
    
    Optional<User> findByGithubUsername(String githubUsername);
    
    boolean existsByEmail(String email);
    
    boolean existsByGithubUsername(String githubUsername);
}
