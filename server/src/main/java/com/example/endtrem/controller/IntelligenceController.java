package com.example.endtrem.controller;

import com.example.endtrem.model.MarketDemand;
import com.example.endtrem.model.RoleInference;
import com.example.endtrem.model.SkillProgression;
import com.example.endtrem.service.IntelligenceLayerService;
import com.example.endtrem.service.MarketDemandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.endtrem.security.UserPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * REST Controller for Phase-2 Intelligence Layer endpoints.
 * Provides access to skill progression tracking, role inference, and market insights.
 */
@Slf4j
@RestController
@RequestMapping("/api/intelligence")
@RequiredArgsConstructor
public class IntelligenceController {
    
    private final IntelligenceLayerService intelligenceService;
    private final MarketDemandService marketDemandService;

    private List<String> candidateUserIds(UserPrincipal principal) {
        if (principal.getUsername() != null && !principal.getUsername().equals(principal.getId())) {
            return List.of(principal.getId(), principal.getUsername());
        }
        return List.of(principal.getId());
    }

    private <T> Optional<T> firstPresent(UserPrincipal principal, Function<String, Optional<T>> loader) {
        for (String id : candidateUserIds(principal)) {
            Optional<T> result = loader.apply(id);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    private <T> List<T> firstNonEmpty(UserPrincipal principal, Function<String, List<T>> loader) {
        for (String id : candidateUserIds(principal)) {
            List<T> result = loader.apply(id);
            if (!result.isEmpty()) {
                return result;
            }
        }
        return List.of();
    }
    
    // ============================================
    // SKILL PROGRESSION ENDPOINTS
    // ============================================
    
    /**
     * Get the latest skill progression analysis.
     */
    @GetMapping("/progression")
    public ResponseEntity<?> getLatestProgression(
            @AuthenticationPrincipal UserPrincipal principal) {
        Optional<SkillProgression> progression = firstPresent(principal, intelligenceService::getLatestProgression);

        if (progression.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "message", "No progression data available. Need at least 2 profile analyses.",
                "available", false
            ));
        }

        return ResponseEntity.ok(progression.get());
    }
    
    /**
     * Compute new progression between latest profiles.
     */
    @PostMapping("/progression/compute")
    public ResponseEntity<?> computeProgression(
            @AuthenticationPrincipal UserPrincipal principal) {
        Optional<SkillProgression> progression = firstPresent(principal, intelligenceService::computeLatestProgression);

        if (progression.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Cannot compute progression",
                "reason", "Need at least 2 skill profiles to compare"
            ));
        }

        return ResponseEntity.ok(progression.get());
    }
    
    /**
     * Get progression history.
     */
    @GetMapping("/progression/history")
    public ResponseEntity<List<SkillProgression>> getProgressionHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(firstNonEmpty(principal, intelligenceService::getProgressionHistory));
    }
    
    /**
     * Get progression summary as narrative.
     */
    @GetMapping("/progression/narrative")
    public ResponseEntity<Map<String, String>> getProgressionNarrative(
            @AuthenticationPrincipal UserPrincipal principal) {
        String narrative = intelligenceService.generateProgressionNarrative(
            candidateUserIds(principal).get(0));
        return ResponseEntity.ok(Map.of("narrative", narrative));
    }
    
    // ============================================
    // ROLE INFERENCE ENDPOINTS
    // ============================================
    
    /**
     * Infer current role level and compute distances to target roles.
     */
    @PostMapping("/role/infer")
    public ResponseEntity<RoleInference> inferRole(
            @AuthenticationPrincipal UserPrincipal principal) {
        for (String id : candidateUserIds(principal)) {
            try {
                RoleInference inference = intelligenceService.inferRole(id);
                return ResponseEntity.ok(inference);
            } catch (IllegalStateException ex) {
                // try the next candidate id
            }
        }
        throw new IllegalStateException("No skill profile found for user");
    }
    
    /**
     * Get the latest role inference.
     */
    @GetMapping("/role")
    public ResponseEntity<?> getLatestRoleInference(
            @AuthenticationPrincipal UserPrincipal principal) {
        Optional<RoleInference> inference = firstPresent(principal, intelligenceService::getLatestInference);

        if (inference.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "message", "No role inference available. Run POST /api/intelligence/role/infer first.",
                "available", false
            ));
        }

        return ResponseEntity.ok(inference.get());
    }
    
    /**
     * Get role inference history.
     */
    @GetMapping("/role/history")
    public ResponseEntity<List<RoleInference>> getRoleInferenceHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(firstNonEmpty(principal, intelligenceService::getInferenceHistory));
    }
    
    /**
     * Get distance to a specific target role.
     */
    @GetMapping("/role/distance/{roleId}")
    public ResponseEntity<?> getDistanceToRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String roleId) {
        for (String id : candidateUserIds(principal)) {
            Optional<RoleInference.RoleDistance> distance = intelligenceService.getDistanceToRole(id, roleId);
            if (distance.isPresent()) {
                return ResponseEntity.ok(distance.get());
            }
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Get the closest reachable role.
     */
    @GetMapping("/role/closest")
    public ResponseEntity<?> getClosestRole(
            @AuthenticationPrincipal UserPrincipal principal) {
        Optional<RoleInference.RoleDistance> closest = firstPresent(principal, intelligenceService::getClosestRole);

        if (closest.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "message", "No role analysis available",
                "available", false
            ));
        }

        return ResponseEntity.ok(closest.get());
    }
    
    /**
     * Get role analysis as narrative.
     */
    @GetMapping("/role/narrative")
    public ResponseEntity<Map<String, String>> getRoleNarrative(
            @AuthenticationPrincipal UserPrincipal principal) {
        String narrative = intelligenceService.generateRoleNarrative(candidateUserIds(principal).get(0));
        return ResponseEntity.ok(Map.of("narrative", narrative));
    }
    
    // ============================================
    // MARKET INSIGHTS ENDPOINTS
    // ============================================
    
    /**
     * Get market demand for a specific skill.
     */
    @GetMapping("/market/skill/{skillId}")
    public ResponseEntity<?> getSkillMarketDemand(@PathVariable String skillId) {
        Optional<MarketDemand> demand = marketDemandService.getDemand(skillId);
        
        if (demand.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "skillId", skillId,
                "message", "Market data not available for this skill",
                "defaultDemandScore", 50
            ));
        }
        
        return ResponseEntity.ok(demand.get());
    }
    
    /**
     * Get top skills by market demand.
     */
    @GetMapping("/market/top")
    public ResponseEntity<List<MarketDemand>> getTopDemandSkills(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(marketDemandService.getTopDemandSkills(limit));
    }
    
    /**
     * Get rising skills (highest growth rate).
     */
    @GetMapping("/market/rising")
    public ResponseEntity<List<MarketDemand>> getRisingSkills(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(marketDemandService.getRisingSkills(limit));
    }
    
    /**
     * Get skills by category.
     */
    @GetMapping("/market/category/{category}")
    public ResponseEntity<List<MarketDemand>> getSkillsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(marketDemandService.getSkillsByCategory(category));
    }
    
    /**
     * Explain why a skill matters now (combines market + role + progression context).
     */
    @GetMapping("/explain/{skillId}")
    public ResponseEntity<Map<String, String>> explainWhySkillMattersNow(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String skillId) {
        String userId = principal.getId();
        String explanation = intelligenceService.explainWhySkillMattersNow(skillId, userId);
        return ResponseEntity.ok(Map.of("explanation", explanation));
    }
    
    // ============================================
    // COMPREHENSIVE REPORT ENDPOINTS
    // ============================================
    
    /**
     * Generate complete intelligence report combining all insights.
     */
    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getIntelligenceReport(
            @AuthenticationPrincipal UserPrincipal principal) {
        String userId = candidateUserIds(principal).get(0);
        
        Map<String, Object> report = new HashMap<>();
        
        // Add components
        report.put("fullReport", intelligenceService.generateIntelligenceReport(userId));
        
        // Add structured data
        intelligenceService.getLatestProgression(userId)
            .ifPresent(p -> report.put("progression", p));
        
        intelligenceService.getLatestInference(userId)
            .ifPresent(i -> report.put("roleInference", i));
        
        report.put("topDemandSkills", marketDemandService.getTopDemandSkills(5));
        report.put("risingSkills", marketDemandService.getRisingSkills(5));
        
        return ResponseEntity.ok(report);
    }
    
    /**
     * Get a quick status summary.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getQuickStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> status = new HashMap<>();

        Optional<SkillProgression> progression = firstPresent(principal, intelligenceService::getLatestProgression);
        Optional<RoleInference> inference = firstPresent(principal, intelligenceService::getLatestInference);

        progression.ifPresent(p -> {
            status.put("progressionStatus", p.getSummary().getOverallStatus().toString());
            status.put("progressScore", p.getSummary().getOverallProgressScore());
            status.put("skillsImproved", p.getSummary().getTotalSkillsImproved());
            status.put("newSkillsAcquired", p.getSummary().getNewSkillsAcquired());
        });

        inference.ifPresent(i -> {
            status.put("inferredLevel", i.getInferredLevel().getLevel());
            status.put("levelConfidence", i.getInferredLevel().getConfidence());

            i.getRoleDistances().stream()
                .min((a, b) -> Double.compare(a.getOverallDistance(), b.getOverallDistance()))
                .ifPresent(closest -> {
                    status.put("closestRole", closest.getRoleName());
                    status.put("distanceToClosest", closest.getOverallDistance());
                    status.put("weeksToClosest", closest.getEstimatedWeeksToReach());
                });
        });

        status.put("hasProgressionData", progression.isPresent());
        status.put("hasRoleInference", inference.isPresent());

        return ResponseEntity.ok(status);
    }
}
