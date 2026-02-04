package com.example.endtrem.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Skill Ontology - Defines the skill dependency graph and relationships.
 * This is a deterministic, rule-based system for understanding skill relationships.
 */
@Component
public class SkillOntology {
    
    private final Map<String, SkillNode> skillGraph = new HashMap<>();
    private final Map<String, List<String>> aliasMap = new HashMap<>();
    private final Map<String, String> categoryMap = new HashMap<>();
    private final Map<String, String> subcategoryMap = new HashMap<>();
    
    @PostConstruct
    public void initialize() {
        initializeLanguages();
        initializeFrameworks();
        initializeDatabases();
        initializeInfrastructure();
        initializeConceptsAndMethodologies();
        initializeTools();
        buildDependencies();
    }
    
    // ==================== SKILL NODE ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillNode {
        private String id;
        private String name;
        private String category;
        private String subcategory;
        private List<String> aliases;
        private List<String> prerequisites; // Skills needed before learning this
        private List<String> enables; // Skills this unlocks
        private List<String> relatedSkills; // Skills often used together
        private int baseDifficulty; // 1-100
        private String learningTime; // Estimated time to learn
        private int marketDemand; // 1-100 job market demand
    }
    
    // ==================== INITIALIZATION ====================
    
    private void initializeLanguages() {
        // Core Languages
        addSkill("javascript", "JavaScript", "language", "web", 
            List.of("js", "es6", "ecmascript"), 
            List.of(), 30, "2-4 weeks", 95);
        
        addSkill("typescript", "TypeScript", "language", "web",
            List.of("ts"),
            List.of("javascript"), 40, "2-3 weeks", 90);
        
        addSkill("python", "Python", "language", "general",
            List.of("py", "python3"),
            List.of(), 25, "3-4 weeks", 95);
        
        addSkill("java", "Java", "language", "enterprise",
            List.of("jdk", "jre"),
            List.of(), 45, "6-8 weeks", 85);
        
        addSkill("go", "Go", "language", "systems",
            List.of("golang"),
            List.of(), 40, "4-6 weeks", 75);
        
        addSkill("rust", "Rust", "language", "systems",
            List.of(),
            List.of(), 70, "8-12 weeks", 65);
        
        addSkill("csharp", "C#", "language", "enterprise",
            List.of("c#", "dotnet"),
            List.of(), 45, "6-8 weeks", 80);
        
        addSkill("kotlin", "Kotlin", "language", "jvm",
            List.of(),
            List.of("java"), 35, "3-4 weeks", 70);
        
        addSkill("ruby", "Ruby", "language", "scripting",
            List.of(),
            List.of(), 30, "3-4 weeks", 55);
        
        addSkill("php", "PHP", "language", "web",
            List.of(),
            List.of(), 30, "3-4 weeks", 60);
        
        addSkill("swift", "Swift", "language", "mobile",
            List.of(),
            List.of(), 40, "4-6 weeks", 65);
        
        addSkill("dart", "Dart", "language", "mobile",
            List.of(),
            List.of(), 35, "3-4 weeks", 60);
    }
    
    private void initializeFrameworks() {
        // JavaScript/TypeScript Frameworks
        addSkill("react", "React", "framework", "frontend",
            List.of("reactjs", "react.js"),
            List.of("javascript", "html", "css"), 45, "4-6 weeks", 95);
        
        addSkill("nextjs", "Next.js", "framework", "fullstack",
            List.of("next.js", "next"),
            List.of("react"), 50, "3-4 weeks", 85);
        
        addSkill("vue", "Vue.js", "framework", "frontend",
            List.of("vuejs", "vue.js"),
            List.of("javascript", "html", "css"), 40, "3-5 weeks", 75);
        
        addSkill("angular", "Angular", "framework", "frontend",
            List.of("angularjs"),
            List.of("typescript", "html", "css"), 55, "6-8 weeks", 70);
        
        addSkill("svelte", "Svelte", "framework", "frontend",
            List.of("sveltekit"),
            List.of("javascript", "html", "css"), 35, "2-3 weeks", 50);
        
        addSkill("express", "Express.js", "framework", "backend",
            List.of("expressjs", "express.js"),
            List.of("javascript", "nodejs"), 35, "2-3 weeks", 80);
        
        addSkill("nestjs", "NestJS", "framework", "backend",
            List.of("nest.js", "nest"),
            List.of("typescript", "nodejs"), 50, "4-6 weeks", 65);
        
        // Java Frameworks
        addSkill("spring", "Spring", "framework", "backend",
            List.of("spring framework"),
            List.of("java"), 55, "6-8 weeks", 85);
        
        addSkill("springboot", "Spring Boot", "framework", "backend",
            List.of("spring boot", "spring-boot"),
            List.of("spring", "java"), 50, "4-6 weeks", 90);
        
        addSkill("hibernate", "Hibernate", "framework", "backend",
            List.of("jpa"),
            List.of("java", "sql"), 45, "3-4 weeks", 70);
        
        // Python Frameworks
        addSkill("django", "Django", "framework", "backend",
            List.of(),
            List.of("python"), 45, "4-6 weeks", 75);
        
        addSkill("flask", "Flask", "framework", "backend",
            List.of(),
            List.of("python"), 30, "2-3 weeks", 65);
        
        addSkill("fastapi", "FastAPI", "framework", "backend",
            List.of("fast-api"),
            List.of("python"), 35, "2-4 weeks", 70);
        
        // Mobile Frameworks
        addSkill("reactnative", "React Native", "framework", "mobile",
            List.of("react-native", "react native"),
            List.of("react", "javascript"), 50, "4-6 weeks", 75);
        
        addSkill("flutter", "Flutter", "framework", "mobile",
            List.of(),
            List.of("dart"), 45, "4-6 weeks", 70);
        
        // .NET Frameworks
        addSkill("aspnet", "ASP.NET", "framework", "backend",
            List.of("asp.net", "asp.net core", ".net core"),
            List.of("csharp"), 50, "5-7 weeks", 75);
    }
    
    private void initializeDatabases() {
        addSkill("postgresql", "PostgreSQL", "database", "relational",
            List.of("postgres", "psql"),
            List.of("sql"), 40, "2-4 weeks", 90);
        
        addSkill("mysql", "MySQL", "database", "relational",
            List.of(),
            List.of("sql"), 35, "2-3 weeks", 80);
        
        addSkill("mongodb", "MongoDB", "database", "nosql",
            List.of("mongo"),
            List.of(), 35, "2-3 weeks", 80);
        
        addSkill("redis", "Redis", "database", "cache",
            List.of(),
            List.of(), 30, "1-2 weeks", 75);
        
        addSkill("elasticsearch", "Elasticsearch", "database", "search",
            List.of("elastic"),
            List.of(), 50, "3-4 weeks", 65);
        
        addSkill("dynamodb", "DynamoDB", "database", "nosql",
            List.of(),
            List.of("aws"), 40, "2-3 weeks", 60);
        
        addSkill("sql", "SQL", "concept", "database",
            List.of(),
            List.of(), 30, "2-3 weeks", 95);
    }
    
    private void initializeInfrastructure() {
        // Containerization
        addSkill("docker", "Docker", "tool", "devops",
            List.of("dockerfile", "docker-compose"),
            List.of(), 40, "2-3 weeks", 90);
        
        addSkill("kubernetes", "Kubernetes", "tool", "devops",
            List.of("k8s"),
            List.of("docker"), 65, "6-8 weeks", 85);
        
        // Cloud Providers
        addSkill("aws", "AWS", "platform", "cloud",
            List.of("amazon web services"),
            List.of(), 55, "8-12 weeks", 95);
        
        addSkill("azure", "Azure", "platform", "cloud",
            List.of("microsoft azure"),
            List.of(), 55, "8-12 weeks", 85);
        
        addSkill("gcp", "Google Cloud", "platform", "cloud",
            List.of("google cloud platform", "gcloud"),
            List.of(), 55, "8-12 weeks", 75);
        
        // CI/CD
        addSkill("github_actions", "GitHub Actions", "tool", "cicd",
            List.of("github-actions", "gh actions"),
            List.of("git"), 35, "1-2 weeks", 80);
        
        addSkill("jenkins", "Jenkins", "tool", "cicd",
            List.of(),
            List.of(), 45, "2-4 weeks", 65);
        
        addSkill("gitlab_ci", "GitLab CI", "tool", "cicd",
            List.of("gitlab-ci"),
            List.of("git"), 40, "2-3 weeks", 60);
        
        // IaC
        addSkill("terraform", "Terraform", "tool", "devops",
            List.of("tf"),
            List.of(), 50, "4-6 weeks", 80);
        
        addSkill("ansible", "Ansible", "tool", "devops",
            List.of(),
            List.of(), 45, "3-4 weeks", 60);
    }
    
    private void initializeConceptsAndMethodologies() {
        // Core Concepts
        addSkill("rest_api", "REST API", "concept", "architecture",
            List.of("restful", "rest"),
            List.of(), 35, "2-3 weeks", 95);
        
        addSkill("graphql", "GraphQL", "concept", "architecture",
            List.of(),
            List.of("rest_api"), 45, "2-4 weeks", 70);
        
        addSkill("microservices", "Microservices", "concept", "architecture",
            List.of("microservice"),
            List.of("rest_api", "docker"), 60, "6-8 weeks", 80);
        
        addSkill("testing", "Testing", "methodology", "quality",
            List.of("unit testing", "tdd", "test-driven"),
            List.of(), 40, "3-4 weeks", 90);
        
        addSkill("cicd", "CI/CD", "methodology", "devops",
            List.of("continuous integration", "continuous deployment"),
            List.of("git", "testing"), 45, "3-4 weeks", 85);
        
        addSkill("agile", "Agile", "methodology", "process",
            List.of("scrum", "kanban"),
            List.of(), 25, "1-2 weeks", 80);
        
        // Security
        addSkill("authentication", "Authentication", "concept", "security",
            List.of("auth", "login", "oauth", "jwt"),
            List.of(), 40, "2-3 weeks", 90);
        
        addSkill("security", "Security Best Practices", "concept", "security",
            List.of("cybersecurity", "appsec"),
            List.of("authentication"), 50, "4-6 weeks", 85);
        
        // Web Fundamentals
        addSkill("html", "HTML", "language", "web",
            List.of("html5"),
            List.of(), 15, "1 week", 95);
        
        addSkill("css", "CSS", "language", "web",
            List.of("css3", "scss", "sass"),
            List.of("html"), 25, "2-3 weeks", 90);
        
        addSkill("git", "Git", "tool", "vcs",
            List.of("github", "gitlab", "version control"),
            List.of(), 25, "1-2 weeks", 98);
        
        addSkill("nodejs", "Node.js", "runtime", "backend",
            List.of("node.js", "node"),
            List.of("javascript"), 35, "2-3 weeks", 90);
    }
    
    private void initializeTools() {
        addSkill("webpack", "Webpack", "tool", "build",
            List.of(),
            List.of("javascript"), 40, "1-2 weeks", 60);
        
        addSkill("vite", "Vite", "tool", "build",
            List.of(),
            List.of("javascript"), 30, "1 week", 65);
        
        addSkill("nginx", "Nginx", "tool", "server",
            List.of(),
            List.of(), 35, "1-2 weeks", 70);
        
        addSkill("linux", "Linux", "platform", "os",
            List.of("ubuntu", "centos", "debian"),
            List.of(), 40, "4-6 weeks", 85);
    }
    
    private void buildDependencies() {
        // Add enables (reverse of prerequisites)
        for (SkillNode node : skillGraph.values()) {
            for (String prereq : node.getPrerequisites()) {
                SkillNode prereqNode = skillGraph.get(prereq);
                if (prereqNode != null) {
                    if (prereqNode.getEnables() == null) {
                        prereqNode.setEnables(new ArrayList<>());
                    }
                    prereqNode.getEnables().add(node.getId());
                }
            }
        }
        
        // Add related skills
        addRelatedSkills("react", List.of("redux", "nextjs", "typescript"));
        addRelatedSkills("springboot", List.of("hibernate", "postgresql", "docker"));
        addRelatedSkills("docker", List.of("kubernetes", "github_actions"));
        addRelatedSkills("aws", List.of("terraform", "docker", "kubernetes"));
        addRelatedSkills("python", List.of("django", "flask", "fastapi"));
        addRelatedSkills("typescript", List.of("react", "angular", "nestjs"));
    }
    
    // ==================== HELPER METHODS ====================
    
    private void addSkill(String id, String name, String category, String subcategory,
                         List<String> aliases, List<String> prerequisites,
                         int difficulty, String learningTime, int marketDemand) {
        SkillNode node = SkillNode.builder()
            .id(id)
            .name(name)
            .category(category)
            .subcategory(subcategory)
            .aliases(aliases)
            .prerequisites(prerequisites)
            .enables(new ArrayList<>())
            .relatedSkills(new ArrayList<>())
            .baseDifficulty(difficulty)
            .learningTime(learningTime)
            .marketDemand(marketDemand)
            .build();
        
        skillGraph.put(id, node);
        categoryMap.put(id, category);
        subcategoryMap.put(id, subcategory);
        
        // Build alias map for normalization
        aliasMap.put(name.toLowerCase(), List.of(id));
        for (String alias : aliases) {
            aliasMap.put(alias.toLowerCase(), List.of(id));
        }
    }
    
    private void addRelatedSkills(String skillId, List<String> related) {
        SkillNode node = skillGraph.get(skillId);
        if (node != null) {
            node.setRelatedSkills(related);
        }
    }
    
    // ==================== PUBLIC API ====================
    
    /**
     * Normalize a skill name to its canonical ID
     */
    public String normalizeSkillName(String rawSkillName) {
        if (rawSkillName == null) return null;
        String lower = rawSkillName.toLowerCase().trim();
        
        // Direct match in graph
        if (skillGraph.containsKey(lower)) {
            return lower;
        }
        
        // Check aliases
        for (Map.Entry<String, List<String>> entry : aliasMap.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue().get(0);
            }
        }
        
        // No match found - return as-is
        return lower.replaceAll("[^a-z0-9]", "_");
    }
    
    /**
     * Get the display name for a skill
     */
    public String getDisplayName(String skillId) {
        SkillNode node = skillGraph.get(skillId);
        return node != null ? node.getName() : skillId;
    }
    
    /**
     * Get a skill node by ID
     */
    public Optional<SkillNode> getSkill(String skillId) {
        return Optional.ofNullable(skillGraph.get(skillId));
    }
    
    /**
     * Get all prerequisites for a skill (including transitive)
     */
    public List<String> getAllPrerequisites(String skillId) {
        Set<String> result = new LinkedHashSet<>();
        collectPrerequisites(skillId, result);
        return new ArrayList<>(result);
    }
    
    private void collectPrerequisites(String skillId, Set<String> collected) {
        SkillNode node = skillGraph.get(skillId);
        if (node == null) return;
        
        for (String prereq : node.getPrerequisites()) {
            if (!collected.contains(prereq)) {
                collected.add(prereq);
                collectPrerequisites(prereq, collected);
            }
        }
    }
    
    /**
     * Get all skills enabled by learning a skill (including transitive)
     */
    public List<String> getAllUnlocks(String skillId) {
        Set<String> result = new LinkedHashSet<>();
        collectUnlocks(skillId, result);
        return new ArrayList<>(result);
    }
    
    private void collectUnlocks(String skillId, Set<String> collected) {
        SkillNode node = skillGraph.get(skillId);
        if (node == null || node.getEnables() == null) return;
        
        for (String enabled : node.getEnables()) {
            if (!collected.contains(enabled)) {
                collected.add(enabled);
                collectUnlocks(enabled, collected);
            }
        }
    }
    
    /**
     * Get the category for a skill
     */
    public String getCategory(String skillId) {
        return categoryMap.getOrDefault(skillId, "unknown");
    }
    
    /**
     * Get the subcategory for a skill
     */
    public String getSubcategory(String skillId) {
        return subcategoryMap.getOrDefault(skillId, "general");
    }
    
    /**
     * Calculate learning order for a set of skills (topological sort)
     */
    public List<String> getLearningOrder(Set<String> skills) {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> temp = new HashSet<>();
        
        for (String skill : skills) {
            if (!visited.contains(skill)) {
                topologicalSort(skill, skills, visited, temp, result);
            }
        }
        
        return result;
    }
    
    private void topologicalSort(String skill, Set<String> targetSkills,
                                 Set<String> visited, Set<String> temp, List<String> result) {
        if (temp.contains(skill)) return; // Cycle
        if (visited.contains(skill)) return;
        
        temp.add(skill);
        
        SkillNode node = skillGraph.get(skill);
        if (node != null) {
            for (String prereq : node.getPrerequisites()) {
                if (targetSkills.contains(prereq)) {
                    topologicalSort(prereq, targetSkills, visited, temp, result);
                }
            }
        }
        
        temp.remove(skill);
        visited.add(skill);
        result.add(skill);
    }
    
    /**
     * Get all skills in a category
     */
    public List<SkillNode> getSkillsByCategory(String category) {
        return skillGraph.values().stream()
            .filter(s -> s.getCategory().equals(category))
            .collect(Collectors.toList());
    }
    
    /**
     * Check if a skill exists in the ontology
     */
    public boolean hasSkill(String skillId) {
        return skillGraph.containsKey(skillId);
    }
    
    /**
     * Get all skill IDs
     */
    public Set<String> getAllSkillIds() {
        return new HashSet<>(skillGraph.keySet());
    }
    
    /**
     * Get aliases for a skill ID
     */
    public List<String> getAliases(String skillId) {
        return aliasMap.getOrDefault(skillId, List.of());
    }
}
