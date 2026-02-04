package com.example.endtrem.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Stores market demand data for skills and roles.
 * Used for market-aware recommendation weighting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "market_demands")
public class MarketDemand {
    
    @Id
    private String id;
    
    @Indexed
    private String skillId;
    
    private String skillName;
    private String category;
    
    // Current demand metrics
    private DemandMetrics currentDemand;
    
    // Historical trend
    private TrendData trend;
    
    // Role-specific demand
    private Map<String, RoleDemand> roleDemands;
    
    // Geographic variations (optional)
    private Map<String, Double> regionalDemand;
    
    // Last updated
    private Instant lastUpdated;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DemandMetrics {
        private double demandScore; // 0-100
        private double growthRate; // Year-over-year percentage
        private double salaryMultiplier; // 1.0 = average, >1 = premium
        private int jobPostingsCount; // Approximate
        private String demandLevel; // "very_high", "high", "moderate", "low", "declining"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendData {
        private String direction; // "rising", "stable", "declining"
        private double momentum; // -1.0 to 1.0
        private List<Double> historicalScores; // Last 12 months
        private String forecast; // "expected_to_grow", "stable", "expected_to_decline"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleDemand {
        private String roleId;
        private double relevanceScore; // How important is this skill for this role
        private boolean isCritical;
        private double demandBoost; // Extra demand for this skill in this role
    }
}
