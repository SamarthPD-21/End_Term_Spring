package com.example.endtrem.service;

import com.example.endtrem.dto.ParsedReadmeDTO;
import com.example.endtrem.model.Repository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Non-AI README Parser Service
 * Extracts structured data from READMEs using pattern matching and heuristics
 * This reduces AI API calls and provides reliable, consistent extraction
 */
@Slf4j
@Service
public class ReadmeParserService {

    // ==================== LANGUAGE DETECTION ====================
    private static final Map<String, String> LANGUAGE_PATTERNS = Map.ofEntries(
        Map.entry("java", "Java"),
        Map.entry("javascript", "JavaScript"),
        Map.entry("typescript", "TypeScript"),
        Map.entry("python", "Python"),
        Map.entry("go", "Go"),
        Map.entry("golang", "Go"),
        Map.entry("rust", "Rust"),
        Map.entry("ruby", "Ruby"),
        Map.entry("php", "PHP"),
        Map.entry("c#", "C#"),
        Map.entry("csharp", "C#"),
        Map.entry("c++", "C++"),
        Map.entry("cpp", "C++"),
        Map.entry("kotlin", "Kotlin"),
        Map.entry("swift", "Swift"),
        Map.entry("scala", "Scala"),
        Map.entry("dart", "Dart"),
        Map.entry("r ", "R"),
        Map.entry("julia", "Julia"),
        Map.entry("elixir", "Elixir"),
        Map.entry("clojure", "Clojure"),
        Map.entry("haskell", "Haskell")
    );

    // ==================== FRAMEWORK DETECTION ====================
    private static final Map<String, String> FRAMEWORK_PATTERNS = Map.ofEntries(
        // Java/JVM
        Map.entry("spring boot", "Spring Boot"),
        Map.entry("springboot", "Spring Boot"),
        Map.entry("spring-boot", "Spring Boot"),
        Map.entry("spring framework", "Spring"),
        Map.entry("hibernate", "Hibernate"),
        Map.entry("quarkus", "Quarkus"),
        Map.entry("micronaut", "Micronaut"),
        // JavaScript/TypeScript
        Map.entry("react", "React"),
        Map.entry("next.js", "Next.js"),
        Map.entry("nextjs", "Next.js"),
        Map.entry("vue", "Vue.js"),
        Map.entry("nuxt", "Nuxt.js"),
        Map.entry("angular", "Angular"),
        Map.entry("express", "Express.js"),
        Map.entry("nest.js", "NestJS"),
        Map.entry("nestjs", "NestJS"),
        Map.entry("svelte", "Svelte"),
        Map.entry("gatsby", "Gatsby"),
        Map.entry("remix", "Remix"),
        Map.entry("astro", "Astro"),
        // Python
        Map.entry("django", "Django"),
        Map.entry("flask", "Flask"),
        Map.entry("fastapi", "FastAPI"),
        Map.entry("tornado", "Tornado"),
        Map.entry("pyramid", "Pyramid"),
        // Ruby
        Map.entry("rails", "Ruby on Rails"),
        Map.entry("ruby on rails", "Ruby on Rails"),
        Map.entry("sinatra", "Sinatra"),
        // PHP
        Map.entry("laravel", "Laravel"),
        Map.entry("symfony", "Symfony"),
        // Go
        Map.entry("gin", "Gin"),
        Map.entry("echo", "Echo"),
        Map.entry("fiber", "Fiber"),
        // .NET
        Map.entry(".net core", ".NET Core"),
        Map.entry("asp.net", "ASP.NET"),
        Map.entry("blazor", "Blazor"),
        // Mobile
        Map.entry("react native", "React Native"),
        Map.entry("flutter", "Flutter"),
        Map.entry("swiftui", "SwiftUI"),
        Map.entry("jetpack compose", "Jetpack Compose")
    );

    // ==================== DATABASE DETECTION ====================
    private static final Map<String, String> DATABASE_PATTERNS = Map.ofEntries(
        Map.entry("mongodb", "MongoDB"),
        Map.entry("mongoose", "MongoDB"),
        Map.entry("postgresql", "PostgreSQL"),
        Map.entry("postgres", "PostgreSQL"),
        Map.entry("mysql", "MySQL"),
        Map.entry("mariadb", "MariaDB"),
        Map.entry("sqlite", "SQLite"),
        Map.entry("redis", "Redis"),
        Map.entry("elasticsearch", "Elasticsearch"),
        Map.entry("dynamodb", "DynamoDB"),
        Map.entry("cassandra", "Cassandra"),
        Map.entry("neo4j", "Neo4j"),
        Map.entry("couchdb", "CouchDB"),
        Map.entry("firestore", "Firestore"),
        Map.entry("supabase", "Supabase"),
        Map.entry("planetscale", "PlanetScale"),
        Map.entry("cockroachdb", "CockroachDB"),
        Map.entry("prisma", "Prisma ORM"),
        Map.entry("typeorm", "TypeORM"),
        Map.entry("sequelize", "Sequelize"),
        Map.entry("drizzle", "Drizzle ORM")
    );

    // ==================== INFRASTRUCTURE DETECTION ====================
    private static final Map<String, String> INFRA_PATTERNS = Map.ofEntries(
        Map.entry("docker", "Docker"),
        Map.entry("dockerfile", "Docker"),
        Map.entry("docker-compose", "Docker Compose"),
        Map.entry("kubernetes", "Kubernetes"),
        Map.entry("k8s", "Kubernetes"),
        Map.entry("helm", "Helm"),
        Map.entry("terraform", "Terraform"),
        Map.entry("ansible", "Ansible"),
        Map.entry("aws", "AWS"),
        Map.entry("amazon web services", "AWS"),
        Map.entry("azure", "Azure"),
        Map.entry("gcp", "Google Cloud"),
        Map.entry("google cloud", "Google Cloud"),
        Map.entry("firebase", "Firebase"),
        Map.entry("vercel", "Vercel"),
        Map.entry("netlify", "Netlify"),
        Map.entry("heroku", "Heroku"),
        Map.entry("railway", "Railway"),
        Map.entry("digitalocean", "DigitalOcean"),
        Map.entry("nginx", "Nginx"),
        Map.entry("apache", "Apache"),
        Map.entry("cloudflare", "Cloudflare"),
        Map.entry("github actions", "GitHub Actions"),
        Map.entry("gitlab ci", "GitLab CI"),
        Map.entry("jenkins", "Jenkins"),
        Map.entry("circleci", "CircleCI"),
        Map.entry("travis", "Travis CI")
    );

    // ==================== AUTH DETECTION ====================
    private static final Map<String, String> AUTH_PATTERNS = Map.ofEntries(
        Map.entry("jwt", "JWT"),
        Map.entry("json web token", "JWT"),
        Map.entry("oauth", "OAuth"),
        Map.entry("oauth2", "OAuth 2.0"),
        Map.entry("openid", "OpenID Connect"),
        Map.entry("passport", "Passport.js"),
        Map.entry("auth0", "Auth0"),
        Map.entry("firebase auth", "Firebase Auth"),
        Map.entry("clerk", "Clerk"),
        Map.entry("nextauth", "NextAuth.js"),
        Map.entry("keycloak", "Keycloak"),
        Map.entry("cognito", "AWS Cognito"),
        Map.entry("bcrypt", "bcrypt"),
        Map.entry("2fa", "Two-Factor Auth"),
        Map.entry("two-factor", "Two-Factor Auth"),
        Map.entry("mfa", "Multi-Factor Auth"),
        Map.entry("rbac", "Role-Based Access Control"),
        Map.entry("role-based", "Role-Based Access Control"),
        Map.entry("session", "Session Auth"),
        Map.entry("cookie auth", "Cookie Auth")
    );

    // ==================== FEATURE KEYWORDS ====================
    private static final Map<String, List<String>> FEATURE_KEYWORDS = Map.ofEntries(
        Map.entry("authentication", List.of("login", "signup", "sign up", "register", "auth", "authentication", "user management")),
        Map.entry("authorization", List.of("rbac", "role", "permission", "access control", "authorization", "admin panel")),
        Map.entry("api_development", List.of("rest api", "restful", "graphql", "api endpoint", "api documentation", "swagger", "openapi")),
        Map.entry("real_time", List.of("websocket", "socket.io", "real-time", "realtime", "live update", "push notification")),
        Map.entry("file_handling", List.of("file upload", "image upload", "s3", "cloudinary", "multer", "file storage")),
        Map.entry("payment", List.of("stripe", "paypal", "payment", "checkout", "billing", "subscription")),
        Map.entry("email", List.of("email", "sendgrid", "mailgun", "smtp", "nodemailer", "notification")),
        Map.entry("search", List.of("search", "elasticsearch", "algolia", "full-text", "filter", "pagination")),
        Map.entry("caching", List.of("redis", "cache", "caching", "memcached", "cdn")),
        Map.entry("testing", List.of("test", "jest", "junit", "pytest", "mocha", "cypress", "playwright", "unit test", "e2e")),
        Map.entry("ci_cd", List.of("ci/cd", "github actions", "gitlab ci", "jenkins", "pipeline", "deployment", "automated")),
        Map.entry("monitoring", List.of("monitoring", "logging", "sentry", "datadog", "prometheus", "grafana", "observability")),
        Map.entry("microservices", List.of("microservice", "service mesh", "api gateway", "message queue", "kafka", "rabbitmq")),
        Map.entry("security", List.of("security", "encryption", "ssl", "https", "sanitization", "xss", "csrf", "sql injection"))
    );

    /**
     * Parse a repository's README and extract all structured data
     */
    public ParsedReadmeDTO parseReadme(Repository repo) {
        String readme = repo.getReadmeContent();
        String description = repo.getDescription();
        String combined = buildCombinedText(readme, description, repo.getTopics());
        
        // Extract tech stack
        ParsedReadmeDTO.TechStack techStack = extractTechStack(combined, repo.getLanguage());
        
        // Extract project maturity signals
        ParsedReadmeDTO.ProjectMaturity projectMaturity = extractProjectMaturity(combined, readme);
        
        // Extract engineering discipline signals
        ParsedReadmeDTO.EngineeringDiscipline discipline = extractEngineeringDiscipline(combined, readme);
        
        // Calculate vibe coding score
        ParsedReadmeDTO.VibeCodingAnalysis vibeAnalysis = analyzeVibeCoding(readme, repo, techStack, discipline);
        
        // Detect project type
        String projectType = detectProjectType(combined);
        
        // Calculate overall complexity
        ParsedReadmeDTO.ComplexityLevel complexity = calculateComplexity(techStack, projectMaturity, discipline);
        
        return ParsedReadmeDTO.builder()
            .repoName(repo.getName())
            .repoId(repo.getId())
            .techStack(techStack)
            .projectMaturity(projectMaturity)
            .engineeringDiscipline(discipline)
            .vibeCodingAnalysis(vibeAnalysis)
            .projectType(projectType)
            .complexity(complexity)
            .description(description != null ? description : "")
            .hasReadme(readme != null && !readme.trim().isEmpty())
            .readmeLength(readme != null ? readme.length() : 0)
            .build();
    }

    private String buildCombinedText(String readme, String description, List<String> topics) {
        StringBuilder sb = new StringBuilder();
        if (readme != null) sb.append(readme).append(" ");
        if (description != null) sb.append(description).append(" ");
        if (topics != null) sb.append(String.join(" ", topics));
        return sb.toString().toLowerCase();
    }

    /**
     * Extract tech stack from README content
     */
    private ParsedReadmeDTO.TechStack extractTechStack(String combined, String primaryLanguage) {
        Set<String> languages = new HashSet<>();
        Set<String> frameworks = new HashSet<>();
        Set<String> databases = new HashSet<>();
        Set<String> infrastructure = new HashSet<>();
        Set<String> authentication = new HashSet<>();
        Set<String> tools = new HashSet<>();

        // Add primary language
        if (primaryLanguage != null && !primaryLanguage.isEmpty()) {
            languages.add(primaryLanguage);
        }

        // Detect languages
        for (Map.Entry<String, String> entry : LANGUAGE_PATTERNS.entrySet()) {
            if (containsWord(combined, entry.getKey())) {
                languages.add(entry.getValue());
            }
        }

        // Detect frameworks
        for (Map.Entry<String, String> entry : FRAMEWORK_PATTERNS.entrySet()) {
            if (combined.contains(entry.getKey())) {
                frameworks.add(entry.getValue());
            }
        }

        // Detect databases
        for (Map.Entry<String, String> entry : DATABASE_PATTERNS.entrySet()) {
            if (combined.contains(entry.getKey())) {
                databases.add(entry.getValue());
            }
        }

        // Detect infrastructure
        for (Map.Entry<String, String> entry : INFRA_PATTERNS.entrySet()) {
            if (combined.contains(entry.getKey())) {
                infrastructure.add(entry.getValue());
            }
        }

        // Detect auth patterns
        for (Map.Entry<String, String> entry : AUTH_PATTERNS.entrySet()) {
            if (combined.contains(entry.getKey())) {
                authentication.add(entry.getValue());
            }
        }

        // Extract from badges (shields.io pattern)
        extractFromBadges(combined, languages, frameworks, tools);

        return ParsedReadmeDTO.TechStack.builder()
            .languages(new ArrayList<>(languages))
            .frameworks(new ArrayList<>(frameworks))
            .databases(new ArrayList<>(databases))
            .infrastructure(new ArrayList<>(infrastructure))
            .authentication(new ArrayList<>(authentication))
            .tools(new ArrayList<>(tools))
            .build();
    }

    /**
     * Extract technologies from shields.io badges
     */
    private void extractFromBadges(String content, Set<String> languages, Set<String> frameworks, Set<String> tools) {
        // Pattern for shields.io badges: ![badge](https://img.shields.io/badge/...)
        Pattern badgePattern = Pattern.compile("img\\.shields\\.io/badge/([^\\s\\)]+)");
        Matcher matcher = badgePattern.matcher(content);
        
        while (matcher.find()) {
            String badge = matcher.group(1).toLowerCase().replace("-", " ").replace("_", " ");
            // Common badge tech names
            if (badge.contains("java")) languages.add("Java");
            if (badge.contains("python")) languages.add("Python");
            if (badge.contains("node")) tools.add("Node.js");
            if (badge.contains("react")) frameworks.add("React");
            if (badge.contains("spring")) frameworks.add("Spring Boot");
            if (badge.contains("docker")) tools.add("Docker");
        }
    }

    /**
     * Extract project maturity signals
     */
    private ParsedReadmeDTO.ProjectMaturity extractProjectMaturity(String combined, String readme) {
        List<String> featuresImplemented = new ArrayList<>();
        List<String> apiEndpoints = new ArrayList<>();
        boolean hasArchitectureDiagram = false;
        boolean hasScreenshots = false;
        boolean hasApiDocumentation = false;
        boolean hasDemoLink = false;
        int featureDepthScore = 0;

        // Detect implemented features
        for (Map.Entry<String, List<String>> entry : FEATURE_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (combined.contains(keyword)) {
                    featuresImplemented.add(entry.getKey());
                    break;
                }
            }
        }

        // Check for architecture diagram
        hasArchitectureDiagram = combined.contains("architecture") && 
            (combined.contains("diagram") || combined.contains(".png") || combined.contains(".svg") || combined.contains("mermaid"));

        // Check for screenshots
        hasScreenshots = combined.contains("screenshot") || combined.contains("preview") ||
            (combined.contains("![") && (combined.contains(".png") || combined.contains(".gif") || combined.contains(".jpg")));

        // Check for API documentation
        hasApiDocumentation = combined.contains("swagger") || combined.contains("openapi") ||
            combined.contains("api documentation") || combined.contains("api reference") ||
            combined.contains("postman") || combined.contains("api endpoints");

        // Check for demo link
        hasDemoLink = combined.contains("demo") || combined.contains("live") || 
            combined.contains("deployed") || combined.contains("https://") && 
            (combined.contains("vercel.app") || combined.contains("netlify.app") || combined.contains("herokuapp.com"));

        // Extract API endpoints mentioned
        Pattern endpointPattern = Pattern.compile("(get|post|put|patch|delete)\\s+[/\\w]+");
        Matcher matcher = endpointPattern.matcher(combined);
        while (matcher.find() && apiEndpoints.size() < 20) {
            apiEndpoints.add(matcher.group());
        }

        // Calculate feature depth score
        featureDepthScore = featuresImplemented.size() * 10;
        if (hasArchitectureDiagram) featureDepthScore += 15;
        if (hasScreenshots) featureDepthScore += 10;
        if (hasApiDocumentation) featureDepthScore += 20;
        if (apiEndpoints.size() > 5) featureDepthScore += 15;

        // Detect specific engineering patterns mentioned
        List<String> engineeringPatterns = new ArrayList<>();
        if (combined.contains("pagination")) engineeringPatterns.add("Pagination");
        if (combined.contains("filter")) engineeringPatterns.add("Filtering");
        if (combined.contains("sort")) engineeringPatterns.add("Sorting");
        if (combined.contains("validation")) engineeringPatterns.add("Input Validation");
        if (combined.contains("error handling")) engineeringPatterns.add("Error Handling");
        if (combined.contains("rate limit")) engineeringPatterns.add("Rate Limiting");
        if (combined.contains("caching")) engineeringPatterns.add("Caching");
        if (combined.contains("queue") || combined.contains("async")) engineeringPatterns.add("Async Processing");

        return ParsedReadmeDTO.ProjectMaturity.builder()
            .featuresImplemented(featuresImplemented)
            .apiEndpoints(apiEndpoints)
            .engineeringPatterns(engineeringPatterns)
            .hasArchitectureDiagram(hasArchitectureDiagram)
            .hasScreenshots(hasScreenshots)
            .hasApiDocumentation(hasApiDocumentation)
            .hasDemoLink(hasDemoLink)
            .featureDepthScore(featureDepthScore)
            .build();
    }

    /**
     * Extract engineering discipline signals
     */
    private ParsedReadmeDTO.EngineeringDiscipline extractEngineeringDiscipline(String combined, String readme) {
        boolean hasInstallationSteps = false;
        boolean hasEnvironmentVariables = false;
        boolean hasEnvExample = false;
        boolean hasContributing = false;
        boolean hasLicense = false;
        boolean hasTests = false;
        boolean hasCiCd = false;
        int setupQualityScore = 0;
        List<String> documentationSections = new ArrayList<>();
        List<String> qualityIndicators = new ArrayList<>();

        // Check for installation steps
        hasInstallationSteps = combined.contains("installation") || combined.contains("getting started") ||
            combined.contains("npm install") || combined.contains("yarn install") ||
            combined.contains("pip install") || combined.contains("mvn install") ||
            combined.contains("how to run") || combined.contains("setup");

        // Check for environment variables
        hasEnvironmentVariables = combined.contains("environment variable") || combined.contains("env var") ||
            combined.contains(".env") || combined.contains("config") || combined.contains("configuration");

        // Check for .env.example
        hasEnvExample = combined.contains(".env.example") || combined.contains("env.example") ||
            combined.contains("copy .env") || combined.contains("create .env");

        // Check for contributing guidelines
        hasContributing = combined.contains("contributing") || combined.contains("contribution") ||
            combined.contains("pull request") || combined.contains("pr welcome");

        // Check for license
        hasLicense = combined.contains("license") || combined.contains("mit") || combined.contains("apache");

        // Check for tests
        hasTests = combined.contains("test") || combined.contains("jest") || combined.contains("junit") ||
            combined.contains("pytest") || combined.contains("npm test") || combined.contains("mvn test");

        // Check for CI/CD
        hasCiCd = combined.contains("ci/cd") || combined.contains("github actions") || 
            combined.contains("gitlab ci") || combined.contains("jenkins") || combined.contains("pipeline");

        // Detect documentation sections
        String[] sections = {"installation", "usage", "api", "configuration", "deployment", 
            "testing", "contributing", "license", "features", "requirements", "architecture"};
        for (String section : sections) {
            if (combined.contains("## " + section) || combined.contains("# " + section) ||
                combined.contains(section + "\n===") || combined.contains(section + "\n---")) {
                documentationSections.add(section);
            }
        }

        // Also check for markdown headers
        if (readme != null) {
            Pattern headerPattern = Pattern.compile("^#{1,3}\\s+(.+)$", Pattern.MULTILINE);
            Matcher matcher = headerPattern.matcher(readme.toLowerCase());
            while (matcher.find()) {
                String header = matcher.group(1).trim();
                if (!documentationSections.contains(header) && header.length() < 50) {
                    documentationSections.add(header);
                }
            }
        }

        // Detect quality indicators
        if (hasInstallationSteps) qualityIndicators.add("Clear Setup Instructions");
        if (hasEnvExample) qualityIndicators.add("Environment Configuration Template");
        if (hasTests) qualityIndicators.add("Test Coverage");
        if (hasCiCd) qualityIndicators.add("Automated Pipeline");
        if (hasContributing) qualityIndicators.add("Contribution Guidelines");
        if (documentationSections.size() >= 5) qualityIndicators.add("Comprehensive Documentation");
        if (combined.contains("error") && combined.contains("handling")) qualityIndicators.add("Error Handling Documentation");
        if (combined.contains("logging")) qualityIndicators.add("Logging Strategy");

        // Calculate setup quality score
        setupQualityScore = 0;
        if (hasInstallationSteps) setupQualityScore += 20;
        if (hasEnvironmentVariables) setupQualityScore += 10;
        if (hasEnvExample) setupQualityScore += 15;
        if (hasTests) setupQualityScore += 20;
        if (hasCiCd) setupQualityScore += 15;
        if (documentationSections.size() >= 3) setupQualityScore += 10;
        if (documentationSections.size() >= 5) setupQualityScore += 10;

        return ParsedReadmeDTO.EngineeringDiscipline.builder()
            .hasInstallationSteps(hasInstallationSteps)
            .hasEnvironmentVariables(hasEnvironmentVariables)
            .hasEnvExample(hasEnvExample)
            .hasContributing(hasContributing)
            .hasLicense(hasLicense)
            .hasTests(hasTests)
            .hasCiCd(hasCiCd)
            .documentationSections(documentationSections)
            .qualityIndicators(qualityIndicators)
            .setupQualityScore(setupQualityScore)
            .build();
    }

    /**
     * Analyze vibe coding signals
     * Returns a probability score and detailed signals
     */
    private ParsedReadmeDTO.VibeCodingAnalysis analyzeVibeCoding(String readme, Repository repo, 
            ParsedReadmeDTO.TechStack techStack, ParsedReadmeDTO.EngineeringDiscipline discipline) {
        
        List<String> vibeSignals = new ArrayList<>();
        List<String> disciplinedSignals = new ArrayList<>();
        int vibeScore = 50; // Start neutral

        // === VIBE CODING SIGNALS (increase score) ===
        
        // 1. Weak or missing README
        if (readme == null || readme.trim().isEmpty()) {
            vibeSignals.add("No README content");
            vibeScore += 25;
        } else if (readme.length() < 200) {
            vibeSignals.add("Very short README (< 200 chars)");
            vibeScore += 15;
        } else if (readme.length() < 500) {
            vibeSignals.add("Brief README (< 500 chars)");
            vibeScore += 10;
        }

        // 2. No setup instructions
        if (!discipline.isHasInstallationSteps()) {
            vibeSignals.add("No installation/setup instructions");
            vibeScore += 15;
        }

        // 3. Feature buzzwords without detail
        if (readme != null) {
            String lower = readme.toLowerCase();
            boolean hasBuzzwords = lower.contains("full-stack") || lower.contains("fullstack") ||
                lower.contains("with auth") || lower.contains("with database") ||
                lower.contains("complete") || lower.contains("production-ready");
            boolean hasDetail = discipline.getDocumentationSections().size() >= 3;
            
            if (hasBuzzwords && !hasDetail) {
                vibeSignals.add("Marketing buzzwords without technical detail");
                vibeScore += 10;
            }
        }

        // 4. No environment configuration
        if (!discipline.isHasEnvExample() && !discipline.isHasEnvironmentVariables()) {
            vibeSignals.add("No environment configuration mentioned");
            vibeScore += 10;
        }

        // 5. No testing mentioned
        if (!discipline.isHasTests()) {
            vibeSignals.add("No testing mentioned");
            vibeScore += 10;
        }

        // === DISCIPLINED CODING SIGNALS (decrease score) ===

        // 1. Detailed README
        if (readme != null && readme.length() > 2000) {
            disciplinedSignals.add("Comprehensive README");
            vibeScore -= 15;
        }

        // 2. Clear documentation structure
        if (discipline.getDocumentationSections().size() >= 5) {
            disciplinedSignals.add("Well-structured documentation");
            vibeScore -= 15;
        }

        // 3. Environment setup provided
        if (discipline.isHasEnvExample()) {
            disciplinedSignals.add("Environment template provided");
            vibeScore -= 10;
        }

        // 4. Testing present
        if (discipline.isHasTests()) {
            disciplinedSignals.add("Testing mentioned/configured");
            vibeScore -= 15;
        }

        // 5. CI/CD present
        if (discipline.isHasCiCd()) {
            disciplinedSignals.add("CI/CD pipeline configured");
            vibeScore -= 15;
        }

        // 6. Contributing guidelines
        if (discipline.isHasContributing()) {
            disciplinedSignals.add("Contribution guidelines present");
            vibeScore -= 10;
        }

        // 7. Explicit trade-offs or limitations mentioned
        if (readme != null) {
            String lower = readme.toLowerCase();
            if (lower.contains("limitation") || lower.contains("trade-off") || 
                lower.contains("future improvement") || lower.contains("todo") ||
                lower.contains("roadmap") || lower.contains("known issue")) {
                disciplinedSignals.add("Acknowledges limitations/trade-offs");
                vibeScore -= 15;
            }
        }

        // 8. Architecture documentation
        if (readme != null && (readme.toLowerCase().contains("architecture") || 
            readme.toLowerCase().contains("system design"))) {
            disciplinedSignals.add("Architecture documentation present");
            vibeScore -= 10;
        }

        // Clamp score between 0 and 100
        vibeScore = Math.max(0, Math.min(100, vibeScore));

        // Determine coding style label
        String codingStyle;
        if (vibeScore >= 70) {
            codingStyle = "Rapid Prototyping Approach";
        } else if (vibeScore >= 50) {
            codingStyle = "Balanced Approach";
        } else if (vibeScore >= 30) {
            codingStyle = "Structured Development";
        } else {
            codingStyle = "Engineering-Focused Approach";
        }

        return ParsedReadmeDTO.VibeCodingAnalysis.builder()
            .vibeScore(vibeScore)
            .codingStyle(codingStyle)
            .vibeSignals(vibeSignals)
            .disciplinedSignals(disciplinedSignals)
            .build();
    }

    /**
     * Detect project type from content
     */
    private String detectProjectType(String combined) {
        if (combined.contains("api") && (combined.contains("rest") || combined.contains("endpoint"))) {
            return "REST API";
        } else if (combined.contains("graphql")) {
            return "GraphQL API";
        } else if (combined.contains("cli") || combined.contains("command line") || combined.contains("terminal")) {
            return "CLI Tool";
        } else if (combined.contains("library") || combined.contains("package") || combined.contains("sdk")) {
            return "Library/Package";
        } else if (combined.contains("webapp") || combined.contains("web app") || combined.contains("dashboard")) {
            return "Web Application";
        } else if (combined.contains("mobile") || combined.contains("android") || combined.contains("ios") || combined.contains("flutter")) {
            return "Mobile App";
        } else if (combined.contains("desktop") || combined.contains("electron")) {
            return "Desktop App";
        } else if (combined.contains("game") || combined.contains("unity") || combined.contains("godot")) {
            return "Game";
        } else if (combined.contains("bot") || combined.contains("discord") || combined.contains("telegram") || combined.contains("slack")) {
            return "Bot";
        } else if (combined.contains("microservice")) {
            return "Microservice";
        } else if (combined.contains("portfolio") || combined.contains("personal website")) {
            return "Portfolio";
        } else if (combined.contains("ecommerce") || combined.contains("e-commerce") || combined.contains("shop")) {
            return "E-commerce";
        } else if (combined.contains("blog")) {
            return "Blog";
        } else if (combined.contains("template") || combined.contains("boilerplate") || combined.contains("starter")) {
            return "Template/Boilerplate";
        } else if (combined.contains("machine learning") || combined.contains("ml") || combined.contains("ai model")) {
            return "ML/AI Project";
        } else if (combined.contains("data") && (combined.contains("analysis") || combined.contains("pipeline"))) {
            return "Data Pipeline";
        }
        return "General Project";
    }

    /**
     * Calculate overall complexity based on all signals
     */
    private ParsedReadmeDTO.ComplexityLevel calculateComplexity(ParsedReadmeDTO.TechStack techStack,
            ParsedReadmeDTO.ProjectMaturity maturity, ParsedReadmeDTO.EngineeringDiscipline discipline) {
        
        int complexityScore = 0;

        // Tech stack complexity
        complexityScore += techStack.getFrameworks().size() * 5;
        complexityScore += techStack.getDatabases().size() * 5;
        complexityScore += techStack.getInfrastructure().size() * 5;
        complexityScore += techStack.getAuthentication().size() * 5;

        // Maturity complexity
        complexityScore += maturity.getFeaturesImplemented().size() * 3;
        complexityScore += maturity.getEngineeringPatterns().size() * 4;
        if (maturity.isHasArchitectureDiagram()) complexityScore += 10;
        if (maturity.isHasApiDocumentation()) complexityScore += 10;

        // Discipline complexity (indicates more thought went into it)
        complexityScore += discipline.getSetupQualityScore() / 5;
        if (discipline.isHasCiCd()) complexityScore += 10;
        if (discipline.isHasTests()) complexityScore += 10;

        if (complexityScore >= 60) {
            return ParsedReadmeDTO.ComplexityLevel.ADVANCED;
        } else if (complexityScore >= 30) {
            return ParsedReadmeDTO.ComplexityLevel.INTERMEDIATE;
        } else {
            return ParsedReadmeDTO.ComplexityLevel.BEGINNER;
        }
    }

    /**
     * Check if content contains a word (not as part of another word)
     */
    private boolean containsWord(String content, String word) {
        if (word.length() <= 2) {
            // For short words, require word boundaries
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
            return pattern.matcher(content).find();
        }
        return content.contains(word);
    }
}
