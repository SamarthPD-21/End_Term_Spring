package com.example.endtrem.controller;

import com.example.endtrem.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final MongoTemplate mongoTemplate;

    /**
     * Basic health check
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "GitUpskill API");
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    /**
     * Detailed health check including MongoDB connection
     */
    @GetMapping("/detailed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detailedHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "GitUpskill API");

        // Check MongoDB connection
        try {
            String dbName = mongoTemplate.getDb().getName();
            health.put("mongodb", Map.of(
                    "status", "UP",
                    "database", dbName
            ));
        } catch (Exception e) {
            log.error("MongoDB health check failed: {}", e.getMessage());
            health.put("mongodb", Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()
            ));
        }

        // Overall status
        boolean allUp = health.values().stream()
                .filter(v -> v instanceof Map)
                .map(v -> (Map<?, ?>) v)
                .allMatch(m -> "UP".equals(m.get("status")));

        health.put("status", allUp ? "UP" : "DEGRADED");

        return ResponseEntity.ok(ApiResponse.success(health));
    }

    /**
     * MongoDB connection test
     */
    @GetMapping("/mongo")
    public ResponseEntity<ApiResponse<Map<String, Object>>> mongoHealthCheck() {
        try {
            String dbName = mongoTemplate.getDb().getName();
            long collectionCount = mongoTemplate.getCollectionNames().size();
            
            Map<String, Object> mongoHealth = new HashMap<>();
            mongoHealth.put("status", "CONNECTED");
            mongoHealth.put("database", dbName);
            mongoHealth.put("collections", collectionCount);
            mongoHealth.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success("MongoDB connection successful", mongoHealth));
        } catch (Exception e) {
            log.error("MongoDB connection failed: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "DISCONNECTED");
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(503).body(ApiResponse.error("MongoDB connection failed: " + e.getMessage()));
        }
    }
}
