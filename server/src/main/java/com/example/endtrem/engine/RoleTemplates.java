package com.example.endtrem.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Role Templates - Predefined skill requirements for various developer roles.
 * Used for gap analysis and career path recommendations.
 */
@Component
public class RoleTemplates {
    
    private final Map<String, RoleTemplate> templates = new HashMap<>();
    
    @PostConstruct
    public void initialize() {
        initializeBackendRoles();
        initializeFrontendRoles();
        initializeFullstackRoles();
        initializeDevOpsRoles();
        initializeDataRoles();
    }
    
    // ==================== ROLE TEMPLATE ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleTemplate {
        private String id;
        private String name;
        private String description;
        private String experienceLevel; // junior, mid, senior
        private String specialization; // backend, frontend, fullstack, devops, data
        
        private List<SkillRequirement> criticalSkills; // Must have
        private List<SkillRequirement> importantSkills; // Should have
        private List<SkillRequirement> niceToHaveSkills; // Good to have
        
        private List<String> careerPaths; // Roles this can lead to
        private int marketDemand; // 1-100
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillRequirement {
        private String skillId;
        private int minimumLevel; // 1-100
        private String rationale; // Why this skill is needed
    }
    
    // ==================== INITIALIZATION ====================
    
    private void initializeBackendRoles() {
        // Junior Backend Developer
        templates.put("junior_backend", RoleTemplate.builder()
            .id("junior_backend")
            .name("Junior Backend Developer")
            .description("Entry-level backend developer focusing on API development and databases")
            .experienceLevel("junior")
            .specialization("backend")
            .criticalSkills(List.of(
                req("java", 50, "Primary backend language for enterprise applications"),
                req("sql", 50, "Essential for database operations"),
                req("rest_api", 50, "Core skill for API development"),
                req("git", 60, "Version control is fundamental")
            ))
            .importantSkills(List.of(
                req("springboot", 40, "Popular Java framework for rapid development"),
                req("postgresql", 40, "Robust relational database"),
                req("testing", 40, "Quality assurance for code")
            ))
            .niceToHaveSkills(List.of(
                req("docker", 30, "Containerization for consistent environments"),
                req("mongodb", 30, "NoSQL database experience"),
                req("linux", 30, "Server environment familiarity")
            ))
            .careerPaths(List.of("mid_backend", "mid_fullstack"))
            .marketDemand(85)
            .build());
        
        // Mid Backend Developer
        templates.put("mid_backend", RoleTemplate.builder()
            .id("mid_backend")
            .name("Mid-Level Backend Developer")
            .description("Experienced backend developer with strong system design skills")
            .experienceLevel("mid")
            .specialization("backend")
            .criticalSkills(List.of(
                req("java", 70, "Strong proficiency in primary language"),
                req("springboot", 65, "Advanced framework knowledge"),
                req("sql", 70, "Complex query optimization"),
                req("rest_api", 70, "API design best practices"),
                req("testing", 60, "Comprehensive testing strategies")
            ))
            .importantSkills(List.of(
                req("docker", 55, "Containerization for deployment"),
                req("postgresql", 60, "Production database management"),
                req("redis", 50, "Caching strategies"),
                req("microservices", 50, "Distributed systems understanding"),
                req("cicd", 50, "Automated deployment pipelines")
            ))
            .niceToHaveSkills(List.of(
                req("kubernetes", 40, "Container orchestration"),
                req("aws", 40, "Cloud platform experience"),
                req("graphql", 35, "Alternative API paradigms"),
                req("elasticsearch", 35, "Search functionality")
            ))
            .careerPaths(List.of("senior_backend", "architect"))
            .marketDemand(90)
            .build());
        
        // Senior Backend Developer
        templates.put("senior_backend", RoleTemplate.builder()
            .id("senior_backend")
            .name("Senior Backend Developer")
            .description("Senior developer with architecture and leadership capabilities")
            .experienceLevel("senior")
            .specialization("backend")
            .criticalSkills(List.of(
                req("java", 85, "Expert-level language proficiency"),
                req("springboot", 80, "Deep framework expertise"),
                req("microservices", 75, "Distributed system design"),
                req("sql", 80, "Database architecture and optimization"),
                req("testing", 75, "Test architecture and strategy")
            ))
            .importantSkills(List.of(
                req("kubernetes", 60, "Production container orchestration"),
                req("aws", 65, "Cloud architecture"),
                req("cicd", 70, "DevOps practices"),
                req("security", 60, "Security architecture"),
                req("docker", 70, "Advanced containerization")
            ))
            .niceToHaveSkills(List.of(
                req("terraform", 45, "Infrastructure as code"),
                req("graphql", 50, "Advanced API patterns"),
                req("elasticsearch", 50, "Search architecture")
            ))
            .careerPaths(List.of("architect", "tech_lead"))
            .marketDemand(95)
            .build());
    }
    
    private void initializeFrontendRoles() {
        // Junior Frontend Developer
        templates.put("junior_frontend", RoleTemplate.builder()
            .id("junior_frontend")
            .name("Junior Frontend Developer")
            .description("Entry-level frontend developer focusing on UI development")
            .experienceLevel("junior")
            .specialization("frontend")
            .criticalSkills(List.of(
                req("html", 60, "Foundation of web development"),
                req("css", 55, "Styling and layout"),
                req("javascript", 55, "Core programming language"),
                req("git", 60, "Version control")
            ))
            .importantSkills(List.of(
                req("react", 45, "Popular frontend framework"),
                req("typescript", 40, "Type-safe JavaScript")
            ))
            .niceToHaveSkills(List.of(
                req("testing", 30, "Frontend testing"),
                req("rest_api", 35, "API integration")
            ))
            .careerPaths(List.of("mid_frontend", "mid_fullstack"))
            .marketDemand(80)
            .build());
        
        // Mid Frontend Developer
        templates.put("mid_frontend", RoleTemplate.builder()
            .id("mid_frontend")
            .name("Mid-Level Frontend Developer")
            .description("Experienced frontend developer with framework expertise")
            .experienceLevel("mid")
            .specialization("frontend")
            .criticalSkills(List.of(
                req("react", 70, "Advanced React patterns"),
                req("typescript", 65, "Type-safe development"),
                req("css", 70, "Advanced styling"),
                req("javascript", 75, "Deep JS knowledge")
            ))
            .importantSkills(List.of(
                req("nextjs", 55, "SSR and full-stack React"),
                req("testing", 55, "Component and E2E testing"),
                req("rest_api", 60, "API design and integration")
            ))
            .niceToHaveSkills(List.of(
                req("graphql", 40, "GraphQL client"),
                req("docker", 35, "Containerized development"),
                req("cicd", 40, "Deployment automation")
            ))
            .careerPaths(List.of("senior_frontend", "mid_fullstack"))
            .marketDemand(85)
            .build());
    }
    
    private void initializeFullstackRoles() {
        // Junior Fullstack Developer
        templates.put("junior_fullstack", RoleTemplate.builder()
            .id("junior_fullstack")
            .name("Junior Fullstack Developer")
            .description("Entry-level developer working across the stack")
            .experienceLevel("junior")
            .specialization("fullstack")
            .criticalSkills(List.of(
                req("javascript", 55, "Core language for both ends"),
                req("html", 55, "Web fundamentals"),
                req("css", 50, "Styling basics"),
                req("git", 60, "Version control"),
                req("sql", 45, "Database basics")
            ))
            .importantSkills(List.of(
                req("react", 45, "Frontend framework"),
                req("nodejs", 45, "Backend JavaScript"),
                req("express", 40, "Backend framework"),
                req("mongodb", 40, "NoSQL database")
            ))
            .niceToHaveSkills(List.of(
                req("typescript", 35, "Type safety"),
                req("docker", 30, "Containerization")
            ))
            .careerPaths(List.of("mid_fullstack", "mid_frontend", "mid_backend"))
            .marketDemand(85)
            .build());
        
        // Mid Fullstack Developer
        templates.put("mid_fullstack", RoleTemplate.builder()
            .id("mid_fullstack")
            .name("Mid-Level Fullstack Developer")
            .description("Experienced developer proficient in both frontend and backend")
            .experienceLevel("mid")
            .specialization("fullstack")
            .criticalSkills(List.of(
                req("typescript", 65, "Type-safe full-stack development"),
                req("react", 65, "Frontend framework mastery"),
                req("nodejs", 60, "Backend runtime"),
                req("sql", 60, "Database proficiency")
            ))
            .importantSkills(List.of(
                req("nextjs", 55, "Full-stack React framework"),
                req("postgresql", 55, "Production database"),
                req("docker", 50, "Containerization"),
                req("testing", 55, "Full-stack testing"),
                req("rest_api", 65, "API design")
            ))
            .niceToHaveSkills(List.of(
                req("aws", 40, "Cloud deployment"),
                req("redis", 40, "Caching"),
                req("graphql", 40, "Alternative APIs"),
                req("cicd", 45, "Deployment pipelines")
            ))
            .careerPaths(List.of("senior_fullstack", "tech_lead"))
            .marketDemand(90)
            .build());
    }
    
    private void initializeDevOpsRoles() {
        // DevOps Engineer
        templates.put("devops_engineer", RoleTemplate.builder()
            .id("devops_engineer")
            .name("DevOps Engineer")
            .description("Engineer focused on infrastructure and deployment automation")
            .experienceLevel("mid")
            .specialization("devops")
            .criticalSkills(List.of(
                req("docker", 75, "Container technology"),
                req("kubernetes", 65, "Container orchestration"),
                req("linux", 70, "Server administration"),
                req("cicd", 75, "Pipeline automation"),
                req("git", 70, "Version control workflows")
            ))
            .importantSkills(List.of(
                req("aws", 65, "Cloud platform"),
                req("terraform", 60, "Infrastructure as code"),
                req("python", 50, "Scripting and automation")
            ))
            .niceToHaveSkills(List.of(
                req("ansible", 45, "Configuration management"),
                req("security", 50, "Security practices")
            ))
            .careerPaths(List.of("sre", "cloud_architect"))
            .marketDemand(90)
            .build());
    }
    
    private void initializeDataRoles() {
        // Data Engineer
        templates.put("data_engineer", RoleTemplate.builder()
            .id("data_engineer")
            .name("Data Engineer")
            .description("Engineer focused on data pipelines and infrastructure")
            .experienceLevel("mid")
            .specialization("data")
            .criticalSkills(List.of(
                req("python", 70, "Primary language for data"),
                req("sql", 80, "Advanced SQL for data processing"),
                req("postgresql", 65, "Relational databases")
            ))
            .importantSkills(List.of(
                req("docker", 55, "Containerized data services"),
                req("aws", 55, "Cloud data services")
            ))
            .niceToHaveSkills(List.of(
                req("elasticsearch", 45, "Search and analytics")
            ))
            .careerPaths(List.of("senior_data_engineer", "data_architect"))
            .marketDemand(85)
            .build());
    }
    
    // ==================== HELPER METHODS ====================
    
    private SkillRequirement req(String skillId, int minLevel, String rationale) {
        return SkillRequirement.builder()
            .skillId(skillId)
            .minimumLevel(minLevel)
            .rationale(rationale)
            .build();
    }
    
    // ==================== PUBLIC API ====================
    
    /**
     * Get a role template by ID
     */
    public Optional<RoleTemplate> getRole(String roleId) {
        return Optional.ofNullable(templates.get(roleId));
    }
    
    /**
     * Get all available role templates
     */
    public Collection<RoleTemplate> getAllRoles() {
        return templates.values();
    }
    
    /**
     * Get roles by experience level
     */
    public List<RoleTemplate> getRolesByLevel(String level) {
        return templates.values().stream()
            .filter(r -> r.getExperienceLevel().equals(level))
            .toList();
    }
    
    /**
     * Get roles by specialization
     */
    public List<RoleTemplate> getRolesBySpecialization(String specialization) {
        return templates.values().stream()
            .filter(r -> r.getSpecialization().equals(specialization))
            .toList();
    }
    
    /**
     * Find the best matching role for a user's skill profile
     */
    public String findBestMatchingRole(Map<String, Integer> userSkills, String preferredSpecialization) {
        String bestMatch = null;
        double bestScore = 0;
        
        List<RoleTemplate> candidates = preferredSpecialization != null 
            ? getRolesBySpecialization(preferredSpecialization)
            : new ArrayList<>(templates.values());
        
        for (RoleTemplate role : candidates) {
            double score = calculateRoleMatchScore(role, userSkills);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = role.getId();
            }
        }
        
        return bestMatch != null ? bestMatch : "junior_fullstack";
    }
    
    /**
     * Calculate how well a user matches a role (0-100)
     */
    public double calculateRoleMatchScore(RoleTemplate role, Map<String, Integer> userSkills) {
        double criticalScore = calculateCategoryScore(role.getCriticalSkills(), userSkills);
        double importantScore = calculateCategoryScore(role.getImportantSkills(), userSkills);
        double niceToHaveScore = calculateCategoryScore(role.getNiceToHaveSkills(), userSkills);
        
        // Weighted scoring: critical 60%, important 30%, nice-to-have 10%
        return (criticalScore * 0.6) + (importantScore * 0.3) + (niceToHaveScore * 0.1);
    }
    
    private double calculateCategoryScore(List<SkillRequirement> requirements, Map<String, Integer> userSkills) {
        if (requirements == null || requirements.isEmpty()) return 100;
        
        double totalScore = 0;
        for (SkillRequirement req : requirements) {
            int userLevel = userSkills.getOrDefault(req.getSkillId(), 0);
            double skillScore = Math.min(100, (userLevel / (double) req.getMinimumLevel()) * 100);
            totalScore += skillScore;
        }
        
        return totalScore / requirements.size();
    }
    
    /**
     * Get the next role in career progression
     */
    public Optional<RoleTemplate> getNextCareerRole(String currentRoleId) {
        RoleTemplate current = templates.get(currentRoleId);
        if (current == null || current.getCareerPaths() == null || current.getCareerPaths().isEmpty()) {
            return Optional.empty();
        }
        
        // Return the first career path option
        return Optional.ofNullable(templates.get(current.getCareerPaths().get(0)));
    }
    
    /**
     * Get all role IDs
     */
    public Set<String> getAllRoleIds() {
        return new HashSet<>(templates.keySet());
    }
}
