package com.example.endtrem.engine;

import com.example.endtrem.model.SkillProfile;
import com.example.endtrem.model.SkillProgression;
import com.example.endtrem.model.SkillProgression.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tracks skill progression over time by computing deltas between stored skill profiles.
 * Provides insights on whether users are improving, stagnating, or regressing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillProgressionTracker {
    
    private final SkillOntology ontology;
    
    /**
     * Compute progression between two skill profiles.
     */
    public SkillProgression computeProgression(SkillProfile previousProfile, 
                                                SkillProfile currentProfile) {
        log.info("Computing skill progression between profiles: {} -> {}", 
            previousProfile.getId(), currentProfile.getId());
        
        // Convert LocalDateTime to Instant for duration calculation
        Instant prevInstant = previousProfile.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
        Instant currInstant = currentProfile.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
        long daysBetween = Duration.between(prevInstant, currInstant).toDays();
        
        // Compute skill-by-skill deltas
        List<SkillDelta> skillDeltas = computeSkillDeltas(previousProfile, currentProfile);
        
        // Compute engineering progression
        EngineeringProgressionMetrics engineeringProgression = 
            computeEngineeringProgression(previousProfile, currentProfile);
        
        // Compute overall summary
        ProgressionSummary summary = computeSummary(skillDeltas, daysBetween);
        
        // Compute velocity metrics
        VelocityMetrics velocity = computeVelocity(skillDeltas, daysBetween);
        
        // Analyze trends
        TrendAnalysis trends = analyzeTrends(skillDeltas);
        
        return SkillProgression.builder()
            .userId(currentProfile.getUserId())
            .previousProfileId(previousProfile.getId())
            .currentProfileId(currentProfile.getId())
            .previousSnapshotDate(prevInstant)
            .currentSnapshotDate(currInstant)
            .daysBetweenSnapshots(daysBetween)
            .summary(summary)
            .skillDeltas(skillDeltas)
            .engineeringProgression(engineeringProgression)
            .velocity(velocity)
            .trends(trends)
            .createdAt(Instant.now())
            .build();
    }
    
    private List<SkillDelta> computeSkillDeltas(SkillProfile previous, SkillProfile current) {
        List<SkillDelta> deltas = new ArrayList<>();
        
        // Create maps for easy lookup
        Map<String, SkillProfile.DetectedSkill> previousSkills = previous.getDetectedSkills()
            .stream()
            .collect(Collectors.toMap(
                SkillProfile.DetectedSkill::getNormalizedName, 
                s -> s,
                (a, b) -> a // Keep first if duplicate
            ));
        
        Map<String, SkillProfile.DetectedSkill> currentSkills = current.getDetectedSkills()
            .stream()
            .collect(Collectors.toMap(
                SkillProfile.DetectedSkill::getNormalizedName, 
                s -> s,
                (a, b) -> a
            ));
        
        // Find all unique skills across both profiles
        Set<String> allSkillIds = new HashSet<>();
        allSkillIds.addAll(previousSkills.keySet());
        allSkillIds.addAll(currentSkills.keySet());
        
        for (String skillId : allSkillIds) {
            SkillProfile.DetectedSkill prevSkill = previousSkills.get(skillId);
            SkillProfile.DetectedSkill currSkill = currentSkills.get(skillId);
            
            SkillDelta delta = computeSingleSkillDelta(skillId, prevSkill, currSkill);
            deltas.add(delta);
        }
        
        // Sort by absolute proficiency delta (most significant changes first)
        deltas.sort((a, b) -> Integer.compare(
            Math.abs(b.getProficiencyDelta()), 
            Math.abs(a.getProficiencyDelta())
        ));
        
        return deltas;
    }
    
    private SkillDelta computeSingleSkillDelta(String skillId,
                                                SkillProfile.DetectedSkill prev,
                                                SkillProfile.DetectedSkill curr) {
        SkillDelta.SkillDeltaBuilder builder = SkillDelta.builder()
            .normalizedName(skillId);
        
        // New skill acquired
        if (prev == null && curr != null) {
            return builder
                .skillName(curr.getName())
                .category(curr.getCategory())
                .currentProficiency(curr.getProficiencyScore())
                .currentEvidence(curr.getEvidenceStrength())
                .currentProjectCount(curr.getProjectCount())
                .proficiencyDelta(curr.getProficiencyScore())
                .evidenceDelta(curr.getEvidenceStrength())
                .projectCountDelta(curr.getProjectCount())
                .deltaType(DeltaType.NEW_SKILL)
                .insight(String.format("🆕 New skill acquired: %s at proficiency level %d", 
                    curr.getName(), curr.getProficiencyScore()))
                .build();
        }
        
        // Skill abandoned
        if (prev != null && curr == null) {
            return builder
                .skillName(prev.getName())
                .category(prev.getCategory())
                .previousProficiency(prev.getProficiencyScore())
                .previousEvidence(prev.getEvidenceStrength())
                .previousProjectCount(prev.getProjectCount())
                .proficiencyDelta(-prev.getProficiencyScore())
                .evidenceDelta(-prev.getEvidenceStrength())
                .projectCountDelta(-prev.getProjectCount())
                .deltaType(DeltaType.ABANDONED)
                .insight(String.format("⚠️ Skill no longer detected: %s (was at proficiency %d)", 
                    prev.getName(), prev.getProficiencyScore()))
                .build();
        }
        
        // Both exist - compute delta
        int profDelta = curr.getProficiencyScore() - prev.getProficiencyScore();
        int evidDelta = curr.getEvidenceStrength() - prev.getEvidenceStrength();
        int projDelta = curr.getProjectCount() - prev.getProjectCount();
        
        DeltaType deltaType = classifyDelta(profDelta);
        String insight = generateDeltaInsight(curr.getName(), profDelta, projDelta, deltaType);
        
        return builder
            .skillName(curr.getName())
            .category(curr.getCategory())
            .previousProficiency(prev.getProficiencyScore())
            .previousEvidence(prev.getEvidenceStrength())
            .previousProjectCount(prev.getProjectCount())
            .currentProficiency(curr.getProficiencyScore())
            .currentEvidence(curr.getEvidenceStrength())
            .currentProjectCount(curr.getProjectCount())
            .proficiencyDelta(profDelta)
            .evidenceDelta(evidDelta)
            .projectCountDelta(projDelta)
            .deltaType(deltaType)
            .insight(insight)
            .build();
    }
    
    private DeltaType classifyDelta(int proficiencyDelta) {
        if (proficiencyDelta > 15) return DeltaType.SIGNIFICANT_GAIN;
        if (proficiencyDelta > 5) return DeltaType.MODERATE_GAIN;
        if (proficiencyDelta > 0) return DeltaType.SLIGHT_GAIN;
        if (proficiencyDelta >= -5) return DeltaType.STABLE;
        if (proficiencyDelta >= -15) return DeltaType.SLIGHT_DECLINE;
        if (proficiencyDelta >= -25) return DeltaType.MODERATE_DECLINE;
        return DeltaType.SIGNIFICANT_DECLINE;
    }
    
    private String generateDeltaInsight(String skillName, int profDelta, int projDelta, DeltaType type) {
        return switch (type) {
            case SIGNIFICANT_GAIN -> String.format("🚀 Excellent progress in %s (+%d proficiency, +%d projects)", 
                skillName, profDelta, projDelta);
            case MODERATE_GAIN -> String.format("📈 Good improvement in %s (+%d proficiency)", 
                skillName, profDelta);
            case SLIGHT_GAIN -> String.format("➡️ Slight improvement in %s (+%d)", 
                skillName, profDelta);
            case STABLE -> String.format("➖ %s proficiency stable", skillName);
            case SLIGHT_DECLINE -> String.format("📉 Slight decline in %s (%d)", 
                skillName, profDelta);
            case MODERATE_DECLINE -> String.format("⚠️ Moderate decline in %s (%d) - consider refreshing", 
                skillName, profDelta);
            case SIGNIFICANT_DECLINE -> String.format("🔴 Significant decline in %s (%d) - needs attention", 
                skillName, profDelta);
            default -> String.format("%s: %+d proficiency", skillName, profDelta);
        };
    }
    
    private EngineeringProgressionMetrics computeEngineeringProgression(
            SkillProfile previous, SkillProfile current) {
        
        SkillProfile.EngineeringScores prevEng = previous.getEngineeringScores();
        SkillProfile.EngineeringScores currEng = current.getEngineeringScores();
        
        if (prevEng == null || currEng == null) {
            return EngineeringProgressionMetrics.builder()
                .testingScoreDelta(0)
                .documentationScoreDelta(0)
                .ciCdScoreDelta(0)
                .testingTrend("unknown")
                .documentationTrend("unknown")
                .ciCdTrend("unknown")
                .engineeringMaturityInsight("Insufficient data to assess engineering practices progression")
                .build();
        }
        
        double testingDelta = currEng.getTestingScore() - prevEng.getTestingScore();
        double docDelta = currEng.getDocumentationScore() - prevEng.getDocumentationScore();
        double ciCdDelta = currEng.getCiCdScore() - prevEng.getCiCdScore();
        
        String testingTrend = trendFromDelta(testingDelta);
        String docTrend = trendFromDelta(docDelta);
        String ciCdTrend = trendFromDelta(ciCdDelta);
        
        String insight = generateEngineeringInsight(testingDelta, docDelta, ciCdDelta);
        
        return EngineeringProgressionMetrics.builder()
            .testingScoreDelta(testingDelta)
            .documentationScoreDelta(docDelta)
            .ciCdScoreDelta(ciCdDelta)
            .testingTrend(testingTrend)
            .documentationTrend(docTrend)
            .ciCdTrend(ciCdTrend)
            .engineeringMaturityInsight(insight)
            .build();
    }
    
    private String trendFromDelta(double delta) {
        if (delta > 5) return "improving";
        if (delta < -5) return "declining";
        return "stable";
    }
    
    private String generateEngineeringInsight(double testDelta, double docDelta, double ciCdDelta) {
        List<String> insights = new ArrayList<>();
        
        if (testDelta > 10) insights.add("significant improvement in testing practices");
        else if (testDelta < -10) insights.add("testing practices need attention");
        
        if (docDelta > 10) insights.add("documentation quality has improved");
        else if (docDelta < -10) insights.add("documentation quality has declined");
        
        if (ciCdDelta > 10) insights.add("CI/CD maturity has increased");
        else if (ciCdDelta < -10) insights.add("CI/CD practices need reinforcement");
        
        if (insights.isEmpty()) {
            return "Engineering practices remain stable.";
        }
        
        return "Engineering practices update: " + String.join("; ", insights) + ".";
    }
    
    private ProgressionSummary computeSummary(List<SkillDelta> deltas, long daysBetween) {
        int improved = 0, stagnant = 0, regressed = 0, newSkills = 0, abandoned = 0;
        double totalDelta = 0;
        
        for (SkillDelta delta : deltas) {
            switch (delta.getDeltaType()) {
                case NEW_SKILL -> newSkills++;
                case SIGNIFICANT_GAIN, MODERATE_GAIN, SLIGHT_GAIN -> {
                    improved++;
                    totalDelta += delta.getProficiencyDelta();
                }
                case STABLE -> stagnant++;
                case SLIGHT_DECLINE, MODERATE_DECLINE, SIGNIFICANT_DECLINE -> {
                    regressed++;
                    totalDelta += delta.getProficiencyDelta();
                }
                case ABANDONED -> {
                    abandoned++;
                    totalDelta -= 10; // Penalty for abandoning skills
                }
            }
        }
        
        // Add bonus for new skills
        totalDelta += newSkills * 15;
        
        // Normalize to -100 to +100 scale
        double progressScore = Math.max(-100, Math.min(100, totalDelta / Math.max(1, deltas.size()) * 5));
        
        ProgressionStatus status = determineStatus(progressScore);
        String narrative = generateNarrative(status, improved, stagnant, regressed, newSkills, abandoned, daysBetween);
        
        return ProgressionSummary.builder()
            .overallStatus(status)
            .totalSkillsImproved(improved)
            .totalSkillsStagnant(stagnant)
            .totalSkillsRegressed(regressed)
            .newSkillsAcquired(newSkills)
            .skillsAbandoned(abandoned)
            .overallProgressScore(progressScore)
            .narrativeSummary(narrative)
            .build();
    }
    
    private ProgressionStatus determineStatus(double score) {
        if (score > 30) return ProgressionStatus.RAPIDLY_IMPROVING;
        if (score > 10) return ProgressionStatus.IMPROVING;
        if (score > 1) return ProgressionStatus.SLIGHTLY_IMPROVING;
        if (score >= -5) return ProgressionStatus.STAGNATING;
        if (score >= -15) return ProgressionStatus.SLIGHTLY_REGRESSING;
        return ProgressionStatus.REGRESSING;
    }
    
    private String generateNarrative(ProgressionStatus status, int improved, int stagnant, 
                                      int regressed, int newSkills, int abandoned, long days) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(switch (status) {
            case RAPIDLY_IMPROVING -> "🚀 Outstanding progress! ";
            case IMPROVING -> "📈 Good progress over the past " + days + " days. ";
            case SLIGHTLY_IMPROVING -> "➡️ Some progress, but there's room for more. ";
            case STAGNATING -> "⏸️ Skills have remained stable. Consider setting new learning goals. ";
            case SLIGHTLY_REGRESSING -> "📉 Slight decline observed. Time to reinvest in learning? ";
            case REGRESSING -> "🔴 Skills appear to be declining. Consider refreshing core competencies. ";
        });
        
        if (newSkills > 0) {
            sb.append(String.format("You've acquired %d new skill(s). ", newSkills));
        }
        
        if (improved > 0) {
            sb.append(String.format("%d skill(s) improved. ", improved));
        }
        
        if (regressed > 0) {
            sb.append(String.format("%d skill(s) need attention. ", regressed));
        }
        
        if (abandoned > 0) {
            sb.append(String.format("%d skill(s) no longer detected - consider if this is intentional. ", abandoned));
        }
        
        return sb.toString().trim();
    }
    
    private VelocityMetrics computeVelocity(List<SkillDelta> deltas, long daysBetween) {
        if (daysBetween <= 0) daysBetween = 1;
        
        double monthFactor = 30.0 / daysBetween;
        
        long newSkillCount = deltas.stream()
            .filter(d -> d.getDeltaType() == DeltaType.NEW_SKILL)
            .count();
        
        long improvementCount = deltas.stream()
            .filter(d -> d.getDeltaType() == DeltaType.SIGNIFICANT_GAIN || 
                        d.getDeltaType() == DeltaType.MODERATE_GAIN)
            .count();
        
        int totalProjectDelta = deltas.stream()
            .mapToInt(d -> Math.max(0, d.getProjectCountDelta()))
            .sum();
        
        double newSkillsPerMonth = newSkillCount * monthFactor;
        double improvementsPerMonth = improvementCount * monthFactor;
        double projectsPerMonth = totalProjectDelta * monthFactor;
        
        // Estimate days to next level based on current velocity
        int estimatedDays;
        String assessment;
        
        if (newSkillsPerMonth >= 2 && improvementsPerMonth >= 3) {
            estimatedDays = 60;
            assessment = "Fast learner - maintaining excellent momentum";
        } else if (newSkillsPerMonth >= 1 || improvementsPerMonth >= 2) {
            estimatedDays = 120;
            assessment = "Steady progress - on track for advancement";
        } else if (improvementsPerMonth >= 1) {
            estimatedDays = 180;
            assessment = "Moderate pace - consider intensifying learning";
        } else {
            estimatedDays = 365;
            assessment = "Needs momentum - set concrete learning goals";
        }
        
        return VelocityMetrics.builder()
            .newSkillsPerMonth(Math.round(newSkillsPerMonth * 100.0) / 100.0)
            .skillImprovementsPerMonth(Math.round(improvementsPerMonth * 100.0) / 100.0)
            .projectsPerMonth(Math.round(projectsPerMonth * 100.0) / 100.0)
            .velocityTrend(0) // Would need historical velocity data to compute this
            .estimatedDaysToNextLevel(estimatedDays)
            .velocityAssessment(assessment)
            .build();
    }
    
    private TrendAnalysis analyzeTrends(List<SkillDelta> deltas) {
        // Group by category
        Map<String, List<SkillDelta>> byCategory = deltas.stream()
            .filter(d -> d.getCategory() != null)
            .collect(Collectors.groupingBy(SkillDelta::getCategory));
        
        Map<String, CategoryTrend> categoryTrends = new HashMap<>();
        
        for (Map.Entry<String, List<SkillDelta>> entry : byCategory.entrySet()) {
            List<SkillDelta> categoryDeltas = entry.getValue();
            
            double avgDelta = categoryDeltas.stream()
                .mapToInt(SkillDelta::getProficiencyDelta)
                .average()
                .orElse(0);
            
            int improved = (int) categoryDeltas.stream()
                .filter(d -> d.getProficiencyDelta() > 0)
                .count();
            
            int declined = (int) categoryDeltas.stream()
                .filter(d -> d.getProficiencyDelta() < 0)
                .count();
            
            String direction = avgDelta > 2 ? "up" : (avgDelta < -2 ? "down" : "stable");
            
            categoryTrends.put(entry.getKey(), CategoryTrend.builder()
                .category(entry.getKey())
                .averageDelta(Math.round(avgDelta * 100.0) / 100.0)
                .skillsImproved(improved)
                .skillsDeclined(declined)
                .trendDirection(direction)
                .build());
        }
        
        // Find strongest growth areas
        List<String> strongestGrowth = categoryTrends.entrySet().stream()
            .filter(e -> e.getValue().getAverageDelta() > 5)
            .sorted((a, b) -> Double.compare(b.getValue().getAverageDelta(), a.getValue().getAverageDelta()))
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Find areas needing attention
        List<String> needingAttention = categoryTrends.entrySet().stream()
            .filter(e -> e.getValue().getAverageDelta() < -5)
            .sorted(Comparator.comparingDouble(e -> e.getValue().getAverageDelta()))
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Detect patterns
        List<String> patterns = detectPatterns(deltas, categoryTrends);
        
        // Project milestones
        List<String> milestones = projectMilestones(deltas);
        
        return TrendAnalysis.builder()
            .categoryTrends(categoryTrends)
            .strongestGrowthAreas(strongestGrowth)
            .areasNeedingAttention(needingAttention)
            .detectedPatterns(patterns)
            .projectedMilestones(milestones)
            .build();
    }
    
    private List<String> detectPatterns(List<SkillDelta> deltas, Map<String, CategoryTrend> trends) {
        List<String> patterns = new ArrayList<>();
        
        // Check for frontend to fullstack shift
        CategoryTrend frontend = trends.get("framework");
        CategoryTrend backend = trends.get("language");
        if (frontend != null && backend != null) {
            if (frontend.getAverageDelta() > 0 && backend.getAverageDelta() > 0) {
                patterns.add("Developing fullstack capabilities");
            }
        }
        
        // Check for infrastructure focus
        CategoryTrend infra = trends.get("infrastructure");
        if (infra != null && infra.getAverageDelta() > 10) {
            patterns.add("Increasing focus on DevOps/infrastructure");
        }
        
        // Check for rapid skill acquisition
        long newSkills = deltas.stream()
            .filter(d -> d.getDeltaType() == DeltaType.NEW_SKILL)
            .count();
        if (newSkills >= 3) {
            patterns.add("Rapid expansion of skill set");
        }
        
        // Check for deepening expertise
        long significantGains = deltas.stream()
            .filter(d -> d.getDeltaType() == DeltaType.SIGNIFICANT_GAIN)
            .count();
        if (significantGains >= 2) {
            patterns.add("Deepening expertise in existing skills");
        }
        
        return patterns;
    }
    
    private List<String> projectMilestones(List<SkillDelta> deltas) {
        List<String> milestones = new ArrayList<>();
        
        // Find skills close to thresholds
        for (SkillDelta delta : deltas) {
            if (delta.getCurrentProficiency() != null) {
                int current = delta.getCurrentProficiency();
                int velocity = delta.getProficiencyDelta();
                
                if (velocity > 0) {
                    if (current < 70 && current + velocity * 2 >= 70) {
                        milestones.add(String.format("On track to reach advanced level in %s", delta.getSkillName()));
                    }
                    if (current < 50 && current + velocity * 2 >= 50) {
                        milestones.add(String.format("Approaching intermediate level in %s", delta.getSkillName()));
                    }
                }
            }
        }
        
        return milestones.stream().limit(3).collect(Collectors.toList());
    }
}
