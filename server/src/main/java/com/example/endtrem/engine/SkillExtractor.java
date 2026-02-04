package com.example.endtrem.engine;

import com.example.endtrem.dto.AggregatedProfileDTO;
import com.example.endtrem.model.SkillProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Skill Extractor - Deterministically extracts and scores skills from aggregated profile data.
 * This component converts raw parsed data into structured, scored skill profiles.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillExtractor {
    
    private final SkillOntology ontology;
    
    /**
     * Extract and score all skills from an aggregated profile
     */
    public SkillProfile extractSkillProfile(String userId, AggregatedProfileDTO profile) {
        log.info("Extracting skill profile for user: {}", userId);
        
        List<SkillProfile.DetectedSkill> allSkills = new ArrayList<>();
        Map<String, List<SkillProfile.DetectedSkill>> skillsByCategory = new HashMap<>();
        
        // Extract skills from different sources
        allSkills.addAll(extractLanguageSkills(profile));
        allSkills.addAll(extractFrameworkSkills(profile));
        allSkills.addAll(extractDatabaseSkills(profile));
        allSkills.addAll(extractInfrastructureSkills(profile));
        allSkills.addAll(extractToolSkills(profile));
        allSkills.addAll(extractConceptSkills(profile));
        
        // Group by category
        for (SkillProfile.DetectedSkill skill : allSkills) {
            skillsByCategory.computeIfAbsent(skill.getCategory(), k -> new ArrayList<>()).add(skill);
        }
        
        // Calculate engineering scores
        SkillProfile.EngineeringScores engineeringScores = calculateEngineeringScores(profile);
        
        // Collect analyzed repo IDs
        List<String> repoIds = profile.getRepoSnapshots() != null 
            ? profile.getRepoSnapshots().stream()
                .map(AggregatedProfileDTO.RepoSnapshot::getName)
                .collect(Collectors.toList())
            : new ArrayList<>();
        
        return SkillProfile.builder()
            .userId(userId)
            .version(1)
            .detectedSkills(allSkills)
            .skillsByCategory(skillsByCategory)
            .engineeringScores(engineeringScores)
            .totalRepositoriesAnalyzed(profile.getTotalProjects())
            .analyzedRepositoryIds(repoIds)
            .build();
    }
    
    // ==================== LANGUAGE EXTRACTION ====================
    
    private List<SkillProfile.DetectedSkill> extractLanguageSkills(AggregatedProfileDTO profile) {
        List<SkillProfile.DetectedSkill> skills = new ArrayList<>();
        
        if (profile.getLanguages() == null) return skills;
        
        Map<String, Integer> langDist = profile.getLanguageDistribution();
        int maxCount = langDist != null ? langDist.values().stream().mapToInt(Integer::intValue).max().orElse(1) : 1;
        int totalProjects = profile.getTotalProjects();
        
        for (String language : profile.getLanguages()) {
            String normalizedId = ontology.normalizeSkillName(language);
            int projectCount = langDist != null ? langDist.getOrDefault(language, 1) : 1;
            
            // Calculate proficiency based on project count and distribution
            int proficiency = calculateProficiencyScore(projectCount, maxCount, totalProjects);
            int evidenceStrength = calculateEvidenceStrength(projectCount, totalProjects);
            
            skills.add(SkillProfile.DetectedSkill.builder()
                .name(language)
                .normalizedName(normalizedId)
                .category("language")
                .subcategory(ontology.getSubcategory(normalizedId))
                .proficiencyScore(proficiency)
                .projectCount(projectCount)
                .evidenceStrength(evidenceStrength)
                .evidenceSources(getRepoNamesForSkill(language, profile))
                .primaryEvidence(String.format("Found in %d of %d projects (%d%%)", 
                    projectCount, totalProjects, (projectCount * 100) / Math.max(1, totalProjects)))
                .trend(calculateTrend(projectCount, totalProjects))
                .recentProjectCount(projectCount) // Would need date info for actual calculation
                .build());
        }
        
        return skills;
    }
    
    // ==================== FRAMEWORK EXTRACTION ====================
    
    private List<SkillProfile.DetectedSkill> extractFrameworkSkills(AggregatedProfileDTO profile) {
        List<SkillProfile.DetectedSkill> skills = new ArrayList<>();
        
        if (profile.getFrameworks() == null) return skills;
        
        for (String framework : profile.getFrameworks()) {
            String normalizedId = ontology.normalizeSkillName(framework);
            int projectCount = countFrameworkUsage(framework, profile);
            
            // Frameworks generally indicate higher skill if used properly
            int proficiency = calculateFrameworkProficiency(framework, projectCount, profile);
            int evidenceStrength = Math.min(90, 50 + (projectCount * 15));
            
            skills.add(SkillProfile.DetectedSkill.builder()
                .name(framework)
                .normalizedName(normalizedId)
                .category("framework")
                .subcategory(ontology.getSubcategory(normalizedId))
                .proficiencyScore(proficiency)
                .projectCount(projectCount)
                .evidenceStrength(evidenceStrength)
                .evidenceSources(getRepoNamesForSkill(framework, profile))
                .primaryEvidence(String.format("Used %s in %d project(s)", framework, projectCount))
                .trend("stable")
                .recentProjectCount(projectCount)
                .build());
        }
        
        return skills;
    }
    
    // ==================== DATABASE EXTRACTION ====================
    
    private List<SkillProfile.DetectedSkill> extractDatabaseSkills(AggregatedProfileDTO profile) {
        List<SkillProfile.DetectedSkill> skills = new ArrayList<>();
        
        if (profile.getDatabases() == null) return skills;
        
        for (String database : profile.getDatabases()) {
            String normalizedId = ontology.normalizeSkillName(database);
            int projectCount = 1; // Default, could be enhanced with frequency tracking
            
            int proficiency = 50 + (projectCount * 10); // Base + usage bonus
            proficiency = Math.min(80, proficiency); // Cap at 80 without more evidence
            
            skills.add(SkillProfile.DetectedSkill.builder()
                .name(database)
                .normalizedName(normalizedId)
                .category("database")
                .subcategory(ontology.getSubcategory(normalizedId))
                .proficiencyScore(proficiency)
                .projectCount(projectCount)
                .evidenceStrength(60)
                .evidenceSources(new ArrayList<>())
                .primaryEvidence("Database integration detected")
                .trend("stable")
                .recentProjectCount(projectCount)
                .build());
        }
        
        // Add SQL skill if any relational database is used
        boolean hasRelational = profile.getDatabases().stream()
            .anyMatch(db -> db.toLowerCase().contains("postgres") || 
                           db.toLowerCase().contains("mysql") ||
                           db.toLowerCase().contains("sql"));
        
        if (hasRelational && skills.stream().noneMatch(s -> s.getNormalizedName().equals("sql"))) {
            skills.add(SkillProfile.DetectedSkill.builder()
                .name("SQL")
                .normalizedName("sql")
                .category("concept")
                .subcategory("database")
                .proficiencyScore(55)
                .projectCount(1)
                .evidenceStrength(70)
                .evidenceSources(new ArrayList<>())
                .primaryEvidence("Inferred from relational database usage")
                .trend("stable")
                .recentProjectCount(1)
                .build());
        }
        
        return skills;
    }
    
    // ==================== INFRASTRUCTURE EXTRACTION ====================
    
    private List<SkillProfile.DetectedSkill> extractInfrastructureSkills(AggregatedProfileDTO profile) {
        List<SkillProfile.DetectedSkill> skills = new ArrayList<>();
        
        if (profile.getInfrastructure() == null) return skills;
        
        for (String infra : profile.getInfrastructure()) {
            String normalizedId = ontology.normalizeSkillName(infra);
            
            int proficiency = 45; // Infrastructure skills usually need more evidence
            int evidenceStrength = 55;
            
            // Boost proficiency based on engineering metrics
            if (profile.getEngineeringMetrics() != null) {
                if (infra.toLowerCase().contains("docker") && profile.getEngineeringMetrics().getReposWithCiCd() > 0) {
                    proficiency += 15;
                    evidenceStrength += 20;
                }
            }
            
            skills.add(SkillProfile.DetectedSkill.builder()
                .name(infra)
                .normalizedName(normalizedId)
                .category(ontology.getCategory(normalizedId))
                .subcategory(ontology.getSubcategory(normalizedId))
                .proficiencyScore(Math.min(75, proficiency))
                .projectCount(1)
                .evidenceStrength(evidenceStrength)
                .evidenceSources(new ArrayList<>())
                .primaryEvidence("Infrastructure technology detected")
                .trend("stable")
                .recentProjectCount(1)
                .build());
        }
        
        return skills;
    }
    
    // ==================== TOOL EXTRACTION ====================
    
    private List<SkillProfile.DetectedSkill> extractToolSkills(AggregatedProfileDTO profile) {
        List<SkillProfile.DetectedSkill> skills = new ArrayList<>();
        
        if (profile.getTools() == null) return skills;
        
        for (String tool : profile.getTools()) {
            String normalizedId = ontology.normalizeSkillName(tool);
            
            skills.add(SkillProfile.DetectedSkill.builder()
                .name(tool)
                .normalizedName(normalizedId)
                .category("tool")
                .subcategory(ontology.getSubcategory(normalizedId))
                .proficiencyScore(50)
                .projectCount(1)
                .evidenceStrength(50)
                .evidenceSources(new ArrayList<>())
                .primaryEvidence("Tool usage detected in projects")
                .trend("stable")
                .recentProjectCount(1)
                .build());
        }
        
        // Always add Git if we have repos
        if (profile.getTotalProjects() > 0) {
            skills.add(SkillProfile.DetectedSkill.builder()
                .name("Git")
                .normalizedName("git")
                .category("tool")
                .subcategory("vcs")
                .proficiencyScore(65)
                .projectCount(profile.getTotalProjects())
                .evidenceStrength(90)
                .evidenceSources(new ArrayList<>())
                .primaryEvidence("GitHub repositories indicate Git usage")
                .trend("stable")
                .recentProjectCount(profile.getTotalProjects())
                .build());
        }
        
        return skills;
    }
    
    // ==================== CONCEPT EXTRACTION ====================
    
    private List<SkillProfile.DetectedSkill> extractConceptSkills(AggregatedProfileDTO profile) {
        List<SkillProfile.DetectedSkill> skills = new ArrayList<>();
        
        // Extract from features
        if (profile.getFeatures() != null) {
            for (String feature : profile.getFeatures()) {
                addConceptSkillIfRelevant(skills, feature, profile);
            }
        }
        
        // Extract from engineering patterns
        if (profile.getEngineeringPatterns() != null) {
            for (String pattern : profile.getEngineeringPatterns()) {
                addConceptSkillIfRelevant(skills, pattern, profile);
            }
        }
        
        // Infer REST API skill from backend frameworks
        boolean hasBackend = profile.getFrameworks() != null && profile.getFrameworks().stream()
            .anyMatch(f -> f.toLowerCase().contains("spring") || 
                          f.toLowerCase().contains("express") ||
                          f.toLowerCase().contains("django") ||
                          f.toLowerCase().contains("flask") ||
                          f.toLowerCase().contains("fastapi"));
        
        if (hasBackend && skills.stream().noneMatch(s -> s.getNormalizedName().equals("rest_api"))) {
            skills.add(SkillProfile.DetectedSkill.builder()
                .name("REST API")
                .normalizedName("rest_api")
                .category("concept")
                .subcategory("architecture")
                .proficiencyScore(60)
                .projectCount(1)
                .evidenceStrength(75)
                .evidenceSources(new ArrayList<>())
                .primaryEvidence("Inferred from backend framework usage")
                .trend("stable")
                .recentProjectCount(1)
                .build());
        }
        
        // Check for testing skill
        if (profile.getEngineeringMetrics() != null && profile.getEngineeringMetrics().getReposWithTests() > 0) {
            int testingScore = Math.min(80, 30 + (profile.getEngineeringMetrics().getReposWithTests() * 15));
            skills.add(SkillProfile.DetectedSkill.builder()
                .name("Testing")
                .normalizedName("testing")
                .category("methodology")
                .subcategory("quality")
                .proficiencyScore(testingScore)
                .projectCount(profile.getEngineeringMetrics().getReposWithTests())
                .evidenceStrength(70)
                .evidenceSources(new ArrayList<>())
                .primaryEvidence(String.format("Tests detected in %d repositories", 
                    profile.getEngineeringMetrics().getReposWithTests()))
                .trend("stable")
                .recentProjectCount(profile.getEngineeringMetrics().getReposWithTests())
                .build());
        }
        
        // Check for CI/CD skill
        if (profile.getEngineeringMetrics() != null && profile.getEngineeringMetrics().getReposWithCiCd() > 0) {
            int cicdScore = Math.min(75, 30 + (profile.getEngineeringMetrics().getReposWithCiCd() * 15));
            skills.add(SkillProfile.DetectedSkill.builder()
                .name("CI/CD")
                .normalizedName("cicd")
                .category("methodology")
                .subcategory("devops")
                .proficiencyScore(cicdScore)
                .projectCount(profile.getEngineeringMetrics().getReposWithCiCd())
                .evidenceStrength(75)
                .evidenceSources(new ArrayList<>())
                .primaryEvidence(String.format("CI/CD pipelines detected in %d repositories", 
                    profile.getEngineeringMetrics().getReposWithCiCd()))
                .trend("stable")
                .recentProjectCount(profile.getEngineeringMetrics().getReposWithCiCd())
                .build());
        }
        
        return skills;
    }
    
    private void addConceptSkillIfRelevant(List<SkillProfile.DetectedSkill> skills, String feature, AggregatedProfileDTO profile) {
        String lower = feature.toLowerCase();
        
        // Map features to concepts
        Map<String, String[]> featureToSkill = Map.of(
            "authentication", new String[]{"authentication", "concept", "security"},
            "authorization", new String[]{"authentication", "concept", "security"},
            "api_development", new String[]{"rest_api", "concept", "architecture"},
            "microservices", new String[]{"microservices", "concept", "architecture"},
            "real_time", new String[]{"websockets", "concept", "architecture"},
            "security", new String[]{"security", "concept", "security"}
        );
        
        for (Map.Entry<String, String[]> entry : featureToSkill.entrySet()) {
            if (lower.contains(entry.getKey())) {
                String[] skillInfo = entry.getValue();
                if (skills.stream().noneMatch(s -> s.getNormalizedName().equals(skillInfo[0]))) {
                    skills.add(SkillProfile.DetectedSkill.builder()
                        .name(ontology.getDisplayName(skillInfo[0]))
                        .normalizedName(skillInfo[0])
                        .category(skillInfo[1])
                        .subcategory(skillInfo[2])
                        .proficiencyScore(50)
                        .projectCount(1)
                        .evidenceStrength(60)
                        .evidenceSources(new ArrayList<>())
                        .primaryEvidence("Detected from project features")
                        .trend("stable")
                        .recentProjectCount(1)
                        .build());
                }
            }
        }
    }
    
    // ==================== SCORING HELPERS ====================
    
    private int calculateProficiencyScore(int projectCount, int maxCount, int totalProjects) {
        // Base score from 40-90 based on usage
        int baseScore = 40;
        
        // Add points based on relative usage
        if (maxCount > 0) {
            baseScore += (projectCount * 35 / maxCount);
        }
        
        // Add points based on absolute project count
        baseScore += Math.min(15, projectCount * 3);
        
        return Math.min(95, baseScore);
    }
    
    private int calculateEvidenceStrength(int projectCount, int totalProjects) {
        if (totalProjects == 0) return 50;
        
        double ratio = (double) projectCount / totalProjects;
        
        // Base evidence strength
        int strength = 40;
        
        // Add based on ratio
        strength += (int)(ratio * 40);
        
        // Add based on absolute count
        strength += Math.min(20, projectCount * 5);
        
        return Math.min(100, strength);
    }
    
    private String calculateTrend(int projectCount, int totalProjects) {
        // Would need temporal data for real trend analysis
        // For now, use heuristics
        if (projectCount >= 3) return "stable";
        if (projectCount == 1 && totalProjects > 5) return "declining";
        return "stable";
    }
    
    private int countFrameworkUsage(String framework, AggregatedProfileDTO profile) {
        if (profile.getRepoSnapshots() == null) return 1;
        
        int count = 0;
        for (AggregatedProfileDTO.RepoSnapshot snapshot : profile.getRepoSnapshots()) {
            if (snapshot.getMainTechnologies() != null && 
                snapshot.getMainTechnologies().stream()
                    .anyMatch(t -> t.equalsIgnoreCase(framework))) {
                count++;
            }
        }
        return Math.max(1, count);
    }
    
    private int calculateFrameworkProficiency(String framework, int projectCount, AggregatedProfileDTO profile) {
        int base = 45;
        
        // More projects = higher proficiency
        base += Math.min(25, projectCount * 10);
        
        // Check for advanced usage indicators
        if (profile.getEngineeringMetrics() != null) {
            // If they have tests with the framework, likely more proficient
            if (profile.getEngineeringMetrics().getReposWithTests() > 0) {
                base += 10;
            }
        }
        
        // Check project complexity
        if (profile.getComplexityDistribution() != null) {
            int advancedCount = profile.getComplexityDistribution().getOrDefault("ADVANCED", 0);
            if (advancedCount > 0) {
                base += 10;
            }
        }
        
        return Math.min(85, base);
    }
    
    private List<String> getRepoNamesForSkill(String skill, AggregatedProfileDTO profile) {
        if (profile.getRepoSnapshots() == null) return new ArrayList<>();
        
        return profile.getRepoSnapshots().stream()
            .filter(s -> s.getMainTechnologies() != null && 
                        s.getMainTechnologies().stream()
                            .anyMatch(t -> t.equalsIgnoreCase(skill)))
            .map(AggregatedProfileDTO.RepoSnapshot::getName)
            .collect(Collectors.toList());
    }
    
    // ==================== ENGINEERING SCORES ====================
    
    private SkillProfile.EngineeringScores calculateEngineeringScores(AggregatedProfileDTO profile) {
        if (profile.getEngineeringMetrics() == null) {
            return SkillProfile.EngineeringScores.builder()
                .testingScore(0)
                .documentationScore(0)
                .ciCdScore(0)
                .codeStructureScore(0)
                .securityAwarenessScore(0)
                .testingCoverage(0)
                .ciCdCoverage(0)
                .docCoverage(0)
                .build();
        }
        
        AggregatedProfileDTO.EngineeringMetrics metrics = profile.getEngineeringMetrics();
        int total = profile.getTotalProjects();
        
        double testingCoverage = total > 0 ? (double) metrics.getReposWithTests() / total : 0;
        double cicdCoverage = total > 0 ? (double) metrics.getReposWithCiCd() / total : 0;
        double docCoverage = total > 0 ? (double) profile.getReposWithReadme() / total : 0;
        
        int testingScore = (int)(testingCoverage * 100);
        int ciCdScore = (int)(cicdCoverage * 100);
        int documentationScore = (int)(docCoverage * 80) + Math.min(20, metrics.getAvgDocumentationSections() * 4);
        
        // Code structure score from complexity distribution
        int codeStructureScore = 50;
        if (profile.getComplexityDistribution() != null) {
            int advanced = profile.getComplexityDistribution().getOrDefault("ADVANCED", 0);
            int intermediate = profile.getComplexityDistribution().getOrDefault("INTERMEDIATE", 0);
            codeStructureScore = Math.min(100, 30 + (advanced * 15) + (intermediate * 8));
        }
        
        // Security score from authentication patterns
        int securityScore = 0;
        if (profile.getAuthentication() != null && !profile.getAuthentication().isEmpty()) {
            securityScore = Math.min(100, 30 + profile.getAuthentication().size() * 20);
        }
        
        return SkillProfile.EngineeringScores.builder()
            .testingScore(testingScore)
            .documentationScore(documentationScore)
            .ciCdScore(ciCdScore)
            .codeStructureScore(codeStructureScore)
            .securityAwarenessScore(securityScore)
            .testingCoverage(testingCoverage)
            .ciCdCoverage(cicdCoverage)
            .docCoverage(docCoverage)
            .testingDetails(Map.of("reposWithTests", metrics.getReposWithTests()))
            .documentationDetails(Map.of(
                "avgSections", metrics.getAvgDocumentationSections(),
                "avgQualityScore", metrics.getAvgSetupQualityScore()
            ))
            .build();
    }
}
