package com.example.endtrem.service;

import com.example.endtrem.model.MarketDemand;
import com.example.endtrem.model.MarketDemand.*;
import com.example.endtrem.repository.MarketDemandRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for managing market demand data.
 * Provides market-aware weighting for recommendation priorities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDemandService {
    
    private final MarketDemandRepository repository;
    
    // In-memory cache of market demand data
    private Map<String, MarketDemand> demandCache = new HashMap<>();
    
    @PostConstruct
    public void initializeMarketData() {
        log.info("Initializing market demand data...");
        
        // Initialize with current market data (2024-2025)
        // In production, this would be updated from external job market APIs
        
        initializeLanguagesDemand();
        initializeFrameworksDemand();
        initializeDatabasesDemand();
        initializeInfrastructureDemand();
        initializeConceptsDemand();
        
        log.info("Market demand data initialized with {} skills", demandCache.size());
    }
    
    private void initializeLanguagesDemand() {
        // High demand languages
        addDemand("python", "Python", "language", 95, 15, 1.2, "very_high", "rising");
        addDemand("javascript", "JavaScript", "language", 90, 5, 1.0, "very_high", "stable");
        addDemand("typescript", "TypeScript", "language", 92, 25, 1.15, "very_high", "rising");
        addDemand("java", "Java", "language", 85, 2, 1.1, "high", "stable");
        addDemand("go", "Go", "language", 80, 20, 1.25, "high", "rising");
        addDemand("rust", "Rust", "language", 70, 35, 1.3, "high", "rising");
        addDemand("kotlin", "Kotlin", "language", 72, 15, 1.15, "high", "rising");
        addDemand("csharp", "C#", "language", 78, 5, 1.05, "high", "stable");
        addDemand("cpp", "C++", "language", 65, 3, 1.1, "moderate", "stable");
        addDemand("ruby", "Ruby", "language", 45, -10, 0.95, "moderate", "declining");
        addDemand("php", "PHP", "language", 50, -5, 0.9, "moderate", "stable");
        addDemand("swift", "Swift", "language", 60, 8, 1.1, "moderate", "stable");
    }
    
    private void initializeFrameworksDemand() {
        // Frontend frameworks
        addDemand("react", "React", "framework", 95, 8, 1.15, "very_high", "stable");
        addDemand("nextjs", "Next.js", "framework", 88, 30, 1.2, "very_high", "rising");
        addDemand("vue", "Vue.js", "framework", 75, 5, 1.05, "high", "stable");
        addDemand("angular", "Angular", "framework", 70, -5, 1.0, "high", "stable");
        addDemand("svelte", "Svelte", "framework", 55, 40, 1.1, "moderate", "rising");
        
        // Backend frameworks
        addDemand("spring", "Spring Boot", "framework", 85, 5, 1.15, "high", "stable");
        addDemand("django", "Django", "framework", 72, 10, 1.05, "high", "stable");
        addDemand("fastapi", "FastAPI", "framework", 78, 45, 1.15, "high", "rising");
        addDemand("express", "Express.js", "framework", 80, 3, 1.0, "high", "stable");
        addDemand("nestjs", "NestJS", "framework", 70, 35, 1.1, "high", "rising");
        addDemand("flask", "Flask", "framework", 60, -5, 0.95, "moderate", "stable");
        addDemand("rails", "Ruby on Rails", "framework", 45, -15, 0.9, "moderate", "declining");
        addDemand("dotnet", ".NET Core", "framework", 75, 8, 1.1, "high", "stable");
    }
    
    private void initializeDatabasesDemand() {
        addDemand("postgresql", "PostgreSQL", "database", 92, 15, 1.1, "very_high", "rising");
        addDemand("mongodb", "MongoDB", "database", 78, 5, 1.0, "high", "stable");
        addDemand("redis", "Redis", "database", 80, 10, 1.05, "high", "stable");
        addDemand("mysql", "MySQL", "database", 75, 0, 0.95, "high", "stable");
        addDemand("elasticsearch", "Elasticsearch", "database", 70, 5, 1.1, "high", "stable");
        addDemand("dynamodb", "DynamoDB", "database", 68, 20, 1.15, "high", "rising");
        addDemand("cassandra", "Cassandra", "database", 55, 0, 1.1, "moderate", "stable");
        addDemand("sqlite", "SQLite", "database", 50, 0, 0.9, "moderate", "stable");
    }
    
    private void initializeInfrastructureDemand() {
        // Cloud & DevOps
        addDemand("kubernetes", "Kubernetes", "infrastructure", 95, 15, 1.3, "very_high", "rising");
        addDemand("docker", "Docker", "infrastructure", 92, 5, 1.1, "very_high", "stable");
        addDemand("aws", "AWS", "infrastructure", 95, 8, 1.25, "very_high", "stable");
        addDemand("azure", "Azure", "infrastructure", 85, 15, 1.2, "high", "rising");
        addDemand("gcp", "Google Cloud", "infrastructure", 75, 12, 1.15, "high", "rising");
        addDemand("terraform", "Terraform", "infrastructure", 88, 20, 1.25, "very_high", "rising");
        addDemand("ansible", "Ansible", "infrastructure", 65, 0, 1.0, "moderate", "stable");
        addDemand("jenkins", "Jenkins", "infrastructure", 60, -10, 0.9, "moderate", "declining");
        addDemand("github-actions", "GitHub Actions", "infrastructure", 82, 40, 1.1, "high", "rising");
        addDemand("gitlab-ci", "GitLab CI", "infrastructure", 70, 15, 1.05, "high", "rising");
        addDemand("prometheus", "Prometheus", "infrastructure", 72, 18, 1.1, "high", "rising");
        addDemand("grafana", "Grafana", "infrastructure", 70, 15, 1.05, "high", "stable");
    }
    
    private void initializeConceptsDemand() {
        addDemand("microservices", "Microservices", "concept", 85, 5, 1.1, "high", "stable");
        addDemand("rest-api", "REST APIs", "concept", 90, 0, 1.0, "very_high", "stable");
        addDemand("graphql", "GraphQL", "concept", 72, 10, 1.1, "high", "stable");
        addDemand("event-driven", "Event-Driven Architecture", "concept", 75, 20, 1.15, "high", "rising");
        addDemand("ci-cd", "CI/CD", "concept", 92, 10, 1.15, "very_high", "stable");
        addDemand("tdd", "Test-Driven Development", "concept", 68, 5, 1.05, "high", "stable");
        addDemand("agile", "Agile Methodology", "concept", 85, 0, 1.0, "high", "stable");
        addDemand("system-design", "System Design", "concept", 90, 15, 1.2, "very_high", "rising");
        addDemand("security", "Security", "concept", 88, 20, 1.25, "very_high", "rising");
        addDemand("observability", "Observability", "concept", 78, 30, 1.15, "high", "rising");
        
        // AI/ML skills (hot market)
        addDemand("machine-learning", "Machine Learning", "concept", 92, 25, 1.35, "very_high", "rising");
        addDemand("llm", "LLM/GenAI", "concept", 98, 100, 1.5, "very_high", "rising");
        addDemand("data-engineering", "Data Engineering", "concept", 88, 20, 1.25, "very_high", "rising");
    }
    
    private void addDemand(String skillId, String name, String category,
                           int demandScore, double growthRate, double salaryMultiplier,
                           String demandLevel, String trendDirection) {
        
        DemandMetrics metrics = DemandMetrics.builder()
            .demandScore(demandScore)
            .growthRate(growthRate)
            .salaryMultiplier(salaryMultiplier)
            .jobPostingsCount(estimateJobPostings(demandScore))
            .demandLevel(demandLevel)
            .build();
        
        TrendData trend = TrendData.builder()
            .direction(trendDirection)
            .momentum(growthRate / 50.0) // Normalize to -1 to 1
            .historicalScores(generateHistoricalScores(demandScore, growthRate))
            .forecast(predictForecast(trendDirection, growthRate))
            .build();
        
        // Role-specific demands
        Map<String, RoleDemand> roleDemands = generateRoleDemands(skillId, category);
        
        MarketDemand demand = MarketDemand.builder()
            .skillId(skillId)
            .skillName(name)
            .category(category)
            .currentDemand(metrics)
            .trend(trend)
            .roleDemands(roleDemands)
            .lastUpdated(Instant.now())
            .build();
        
        demandCache.put(skillId, demand);
    }
    
    private int estimateJobPostings(int demandScore) {
        // Approximate job postings based on demand score
        return (int) (demandScore * 1000 + Math.random() * 5000);
    }
    
    private List<Double> generateHistoricalScores(int currentScore, double growthRate) {
        List<Double> scores = new ArrayList<>();
        double score = currentScore;
        
        // Generate 12 months of historical data (backwards)
        for (int i = 11; i >= 0; i--) {
            double historicalScore = score - (growthRate / 12.0 * i);
            scores.add(Math.max(0, Math.min(100, historicalScore)));
        }
        
        return scores;
    }
    
    private String predictForecast(String direction, double growthRate) {
        if (growthRate > 20) return "expected_to_grow";
        if (growthRate < -10) return "expected_to_decline";
        return "stable";
    }
    
    private Map<String, RoleDemand> generateRoleDemands(String skillId, String category) {
        Map<String, RoleDemand> demands = new HashMap<>();
        
        // Backend roles
        if (category.equals("language") || category.equals("database") || 
            skillId.contains("spring") || skillId.contains("django") || skillId.contains("express")) {
            demands.put("backend", RoleDemand.builder()
                .roleId("backend")
                .relevanceScore(0.9)
                .isCritical(true)
                .demandBoost(10)
                .build());
        }
        
        // Frontend roles
        if (skillId.contains("react") || skillId.contains("vue") || skillId.contains("angular") ||
            skillId.equals("javascript") || skillId.equals("typescript")) {
            demands.put("frontend", RoleDemand.builder()
                .roleId("frontend")
                .relevanceScore(0.95)
                .isCritical(true)
                .demandBoost(15)
                .build());
        }
        
        // DevOps roles
        if (category.equals("infrastructure")) {
            demands.put("devops", RoleDemand.builder()
                .roleId("devops")
                .relevanceScore(0.95)
                .isCritical(true)
                .demandBoost(20)
                .build());
        }
        
        // Data roles
        if (skillId.contains("python") || skillId.contains("machine-learning") || 
            skillId.contains("data") || category.equals("database")) {
            demands.put("data", RoleDemand.builder()
                .roleId("data")
                .relevanceScore(0.85)
                .isCritical(category.equals("database") || skillId.contains("machine-learning"))
                .demandBoost(15)
                .build());
        }
        
        return demands;
    }
    
    /**
     * Get market demand for a specific skill.
     */
    public Optional<MarketDemand> getDemand(String skillId) {
        // Try cache first
        if (demandCache.containsKey(skillId)) {
            return Optional.of(demandCache.get(skillId));
        }
        
        // Try database
        return repository.findBySkillId(skillId);
    }
    
    /**
     * Get demand score for a skill (0-100).
     */
    public double getDemandScore(String skillId) {
        return getDemand(skillId)
            .map(d -> d.getCurrentDemand().getDemandScore())
            .orElse(50.0); // Default to moderate
    }
    
    /**
     * Get growth rate for a skill.
     */
    public double getGrowthRate(String skillId) {
        return getDemand(skillId)
            .map(d -> d.getCurrentDemand().getGrowthRate())
            .orElse(0.0);
    }
    
    /**
     * Get salary multiplier for a skill.
     */
    public double getSalaryMultiplier(String skillId) {
        return getDemand(skillId)
            .map(d -> d.getCurrentDemand().getSalaryMultiplier())
            .orElse(1.0);
    }
    
    /**
     * Get market insight for a skill.
     */
    public String getMarketInsight(String skillId) {
        Optional<MarketDemand> demandOpt = getDemand(skillId);
        
        if (demandOpt.isEmpty()) {
            return "Market data not available for this skill.";
        }
        
        MarketDemand demand = demandOpt.get();
        DemandMetrics metrics = demand.getCurrentDemand();
        TrendData trend = demand.getTrend();
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s has %s market demand", demand.getSkillName(), metrics.getDemandLevel()));
        
        if (trend.getDirection().equals("rising")) {
            sb.append(String.format(" and is growing at %.0f%% annually", metrics.getGrowthRate()));
        } else if (trend.getDirection().equals("declining")) {
            sb.append(" but demand is declining");
        }
        
        if (metrics.getSalaryMultiplier() > 1.1) {
            sb.append(String.format(". Commands a %.0f%% salary premium", (metrics.getSalaryMultiplier() - 1) * 100));
        }
        
        sb.append(".");
        
        return sb.toString();
    }
    
    /**
     * Compute market-adjusted priority score for a skill recommendation.
     */
    public double computeMarketAdjustedScore(String skillId, double baseScore) {
        Optional<MarketDemand> demandOpt = getDemand(skillId);
        
        if (demandOpt.isEmpty()) {
            return baseScore;
        }
        
        MarketDemand demand = demandOpt.get();
        DemandMetrics metrics = demand.getCurrentDemand();
        TrendData trend = demand.getTrend();
        
        // Market adjustment factors
        double demandFactor = metrics.getDemandScore() / 100.0; // 0-1
        double growthFactor = 1 + (metrics.getGrowthRate() / 100.0); // e.g., 1.15 for 15% growth
        double salaryFactor = metrics.getSalaryMultiplier();
        
        // Momentum bonus for rising skills
        double momentumBonus = trend.getDirection().equals("rising") ? 1.1 : 
                              trend.getDirection().equals("declining") ? 0.9 : 1.0;
        
        // Combined adjustment (weighted)
        double marketMultiplier = (demandFactor * 0.4) + (growthFactor * 0.3) + 
                                  (salaryFactor * 0.2) + (momentumBonus * 0.1);
        
        return baseScore * marketMultiplier;
    }
    
    /**
     * Get top skills by market demand.
     */
    public List<MarketDemand> getTopDemandSkills(int limit) {
        return demandCache.values().stream()
            .sorted((a, b) -> Double.compare(
                b.getCurrentDemand().getDemandScore(), 
                a.getCurrentDemand().getDemandScore()))
            .limit(limit)
            .toList();
    }
    
    /**
     * Get rising skills (highest growth rate).
     */
    public List<MarketDemand> getRisingSkills(int limit) {
        return demandCache.values().stream()
            .filter(d -> d.getTrend().getDirection().equals("rising"))
            .sorted((a, b) -> Double.compare(
                b.getCurrentDemand().getGrowthRate(), 
                a.getCurrentDemand().getGrowthRate()))
            .limit(limit)
            .toList();
    }
    
    /**
     * Get skills by category.
     */
    public List<MarketDemand> getSkillsByCategory(String category) {
        return demandCache.values().stream()
            .filter(d -> d.getCategory().equals(category))
            .sorted((a, b) -> Double.compare(
                b.getCurrentDemand().getDemandScore(), 
                a.getCurrentDemand().getDemandScore()))
            .toList();
    }
    
    /**
     * Generate market context explanation for recommendations.
     */
    public String generateMarketContext(List<String> skillIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Market Context:**\n\n");
        
        List<String> highDemand = new ArrayList<>();
        List<String> rising = new ArrayList<>();
        List<String> premium = new ArrayList<>();
        
        for (String skillId : skillIds) {
            getDemand(skillId).ifPresent(demand -> {
                if (demand.getCurrentDemand().getDemandScore() >= 85) {
                    highDemand.add(demand.getSkillName());
                }
                if (demand.getTrend().getDirection().equals("rising") && 
                    demand.getCurrentDemand().getGrowthRate() > 15) {
                    rising.add(demand.getSkillName());
                }
                if (demand.getCurrentDemand().getSalaryMultiplier() > 1.15) {
                    premium.add(demand.getSkillName());
                }
            });
        }
        
        if (!highDemand.isEmpty()) {
            sb.append("🔥 **High Demand:** ").append(String.join(", ", highDemand)).append("\n");
        }
        if (!rising.isEmpty()) {
            sb.append("📈 **Rising:** ").append(String.join(", ", rising)).append("\n");
        }
        if (!premium.isEmpty()) {
            sb.append("💰 **Premium Skills:** ").append(String.join(", ", premium)).append("\n");
        }
        
        return sb.toString();
    }
}
