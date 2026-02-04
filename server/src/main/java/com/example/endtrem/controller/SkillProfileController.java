package com.example.endtrem.controller;

import com.example.endtrem.engine.RoleTemplates;
import com.example.endtrem.model.RecommendationResult;
import com.example.endtrem.model.SkillProfile;
import com.example.endtrem.security.UserPrincipal;
import com.example.endtrem.service.DeterministicRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller for accessing skill profiles and recommendation results.
 * Provides endpoints to query the deterministic recommendation engine outputs.
 */
@Slf4j
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillProfileController {
    
    private final DeterministicRecommendationService recommendationService;

    private List<String> candidateUserIds(UserPrincipal principal) {
        List<String> ids = new ArrayList<>();
        ids.add(principal.getId());
        if (principal.getUsername() != null && !principal.getUsername().equals(principal.getId())) {
            ids.add(principal.getUsername());
        }
        return ids;
    }
    
    /**
     * Get the latest skill profile for the authenticated user
     */
    @GetMapping("/profile")
    public ResponseEntity<SkillProfile> getLatestSkillProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        for (String userId : candidateUserIds(principal)) {
            var profile = recommendationService.getLatestSkillProfile(userId);
            if (profile.isPresent()) {
                return ResponseEntity.ok(profile.get());
            }
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Get skill profile history for the authenticated user
     */
    @GetMapping("/profile/history")
    public ResponseEntity<List<SkillProfile>> getSkillProfileHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        for (String userId : candidateUserIds(principal)) {
            List<SkillProfile> history = recommendationService.getSkillProfileHistory(userId);
            if (!history.isEmpty()) {
                return ResponseEntity.ok(history);
            }
        }
        return ResponseEntity.ok(List.of());
    }
    
    /**
     * Get the latest recommendations for the authenticated user
     */
    @GetMapping("/recommendations")
    public ResponseEntity<RecommendationResult> getLatestRecommendations(
            @AuthenticationPrincipal UserPrincipal principal) {
        for (String userId : candidateUserIds(principal)) {
            var rec = recommendationService.getLatestRecommendations(userId);
            if (rec.isPresent()) {
                return ResponseEntity.ok(rec.get());
            }
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Get recommendation history for the authenticated user
     */
    @GetMapping("/recommendations/history")
    public ResponseEntity<List<RecommendationResult>> getRecommendationHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        for (String userId : candidateUserIds(principal)) {
            List<RecommendationResult> history = recommendationService.getRecommendationHistory(userId);
            if (!history.isEmpty()) {
                return ResponseEntity.ok(history);
            }
        }
        return ResponseEntity.ok(List.of());
    }
    
    /**
     * Regenerate recommendations for an existing skill profile with a different target role
     */
    @PostMapping("/recommendations/regenerate")
    public ResponseEntity<RecommendationResult> regenerateRecommendations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> request) {
        String skillProfileId = request.get("skillProfileId");
        String targetRoleId = request.get("targetRoleId");
        
        if (skillProfileId == null || skillProfileId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        RecommendationResult result = recommendationService.regenerateRecommendations(
            skillProfileId, targetRoleId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get all available target roles
     */
    @GetMapping("/roles")
    public ResponseEntity<List<RoleTemplates.RoleTemplate>> getAvailableRoles() {
        List<RoleTemplates.RoleTemplate> roles = recommendationService.getAvailableRoles();
        return ResponseEntity.ok(roles);
    }
    
    /**
     * Get roles by specialization (backend, frontend, fullstack, devops, data)
     */
    @GetMapping("/roles/{specialization}")
    public ResponseEntity<List<RoleTemplates.RoleTemplate>> getRolesBySpecialization(
            @PathVariable String specialization) {
        List<RoleTemplates.RoleTemplate> roles = recommendationService.getRolesBySpecialization(specialization);
        return ResponseEntity.ok(roles);
    }
    
    /**
     * Get gap analysis summary for the latest recommendation
     */
    @GetMapping("/gap-analysis")
    public ResponseEntity<RecommendationResult.GapAnalysis> getGapAnalysis(
            @AuthenticationPrincipal UserPrincipal principal) {
        for (String userId : candidateUserIds(principal)) {
            var rec = recommendationService.getLatestRecommendations(userId);
            if (rec.isPresent()) {
                return ResponseEntity.ok(rec.get().getGapAnalysis());
            }
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Get scoring metadata for transparency/explainability
     */
    @GetMapping("/scoring-metadata")
    public ResponseEntity<RecommendationResult.ScoringMetadata> getScoringMetadata(
            @AuthenticationPrincipal UserPrincipal principal) {
        for (String userId : candidateUserIds(principal)) {
            var rec = recommendationService.getLatestRecommendations(userId);
            if (rec.isPresent()) {
                return ResponseEntity.ok(rec.get().getScoringMetadata());
            }
        }
        return ResponseEntity.notFound().build();
    }
}
