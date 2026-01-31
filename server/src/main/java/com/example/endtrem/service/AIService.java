package com.example.endtrem.service;

import com.example.endtrem.config.OpenAIConfig;
import com.example.endtrem.dto.AggregatedProfileDTO;
import com.example.endtrem.model.AnalysisResult;
import com.example.endtrem.model.RepoSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {
    
    private final WebClient openAIWebClient;
    private final OpenAIConfig openAIConfig;
    private final ObjectMapper objectMapper;
    
    /**
     * Summarize a single README into structured data
     */
    public RepoSummary summarizeReadme(String repoName, String readmeContent, String language) {
        if (readmeContent == null || readmeContent.trim().isEmpty()) {
            return createMinimalSummary(repoName, language);
        }
        
        // Truncate README if too long (keep token usage low)
        String truncatedReadme = readmeContent.length() > 3000 
            ? readmeContent.substring(0, 3000) + "..." 
            : readmeContent;
        
        String prompt = String.format("""
            Analyze this GitHub repository README and extract key information.
            Repository Name: %s
            Primary Language: %s
            
            README Content:
            %s
            
            Respond with ONLY valid JSON in this exact format:
            {
                "technologies": ["list of technologies/languages used"],
                "frameworks": ["list of frameworks detected"],
                "features": ["key features implemented"],
                "projectType": "type of project (e.g., REST API, Web App, CLI Tool, Library)",
                "complexity": "BEGINNER or INTERMEDIATE or ADVANCED",
                "description": "one sentence summary",
                "keywords": ["relevant keywords for skill matching"]
            }
            """, repoName, language != null ? language : "Unknown", truncatedReadme);
        
        String response = callOpenAI(prompt, 500);
        
        try {
            JsonNode json = objectMapper.readTree(response);
            return RepoSummary.builder()
                .repoName(repoName)
                .technologies(jsonArrayToList(json.get("technologies")))
                .frameworks(jsonArrayToList(json.get("frameworks")))
                .features(jsonArrayToList(json.get("features")))
                .projectType(json.has("projectType") ? json.get("projectType").asText() : "Unknown")
                .complexity(parseComplexity(json.has("complexity") ? json.get("complexity").asText() : "INTERMEDIATE"))
                .description(json.has("description") ? json.get("description").asText() : "")
                .keywords(jsonArrayToList(json.get("keywords")))
                .build();
        } catch (Exception e) {
            log.error("Failed to parse AI response for {}: {}", repoName, e.getMessage());
            return createMinimalSummary(repoName, language);
        }
    }
    
    /**
     * Analyze aggregated profile and generate skill analysis with enhanced recommendations
     * Now includes pre-parsed README data with vibe coding analysis
     */
    public AnalysisResult analyzeProfile(AggregatedProfileDTO profile) {
        String profileJson;
        try {
            profileJson = objectMapper.writeValueAsString(profile);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize profile", e);
        }
        
        String prompt = String.format("""
            You are an expert developer career advisor. Analyze this developer's GitHub profile which has been pre-analyzed by our non-AI parsing pipeline.
            
            The profile contains:
            - Tech stack extracted from READMEs (languages, frameworks, databases, infrastructure, auth)
            - Engineering discipline metrics (testing, CI/CD, documentation quality)
            - Coding style analysis (structured vs rapid prototyping approach)
            - Per-repository snapshots with complexity and feature depth
            
            Developer Profile Data:
            %s
            
            IMPORTANT CONTEXT FROM PRE-ANALYSIS:
            1. "vibeCodingMetrics.avgVibeScore" indicates coding style (0-100, higher = more rapid prototyping, lower = more structured)
            2. "engineeringMetrics" shows testing, CI/CD, documentation habits
            3. "repoSnapshots" gives per-project breakdown
            4. "potentialGaps" are skill gaps detected by pattern matching
            
            CATEGORIZE RECOMMENDATIONS INTO THREE TYPES:
            1. "learn_new" - Skills the developer should START learning (not in their projects)
            2. "improve_existing" - Skills they have but should DEEPEN (seen but basic/moderate)
            3. "practice_more" - Skills they're good at but need more REAL-WORLD practice
            
            CODING STYLE FEEDBACK RULES:
            - If avgVibeScore >= 60: Suggest improving documentation, testing, and project structure
            - If avgVibeScore <= 40: Acknowledge strong engineering practices, suggest learning new technologies
            - NEVER say "vibe coder" - instead use phrases like "rapid prototyping approach" or "move-fast mindset"
            
            Respond with ONLY valid JSON in this exact format:
            {
                "skillAnalysis": {
                    "strongSkills": [
                        {"name": "skill", "category": "language|framework|tool|concept|methodology", "proficiencyScore": 80, "evidence": "why strong", "projectCount": 5, "trend": "improving|stable|declining"}
                    ],
                    "moderateSkills": [
                        {"name": "skill", "category": "category", "proficiencyScore": 50, "evidence": "why moderate", "projectCount": 2, "trend": "stable"}
                    ],
                    "weakSkills": [
                        {"name": "skill", "category": "category", "proficiencyScore": 30, "evidence": "why weak", "projectCount": 1, "trend": "stable"}
                    ],
                    "missingSkills": ["important skills not found"],
                    "totalSkillsCount": 15
                },
                "enhancedRecommendations": {
                    "skillsToLearn": [
                        {
                            "skill": "new skill",
                            "reason": "why learn this",
                            "priority": 1,
                            "resources": ["courses", "docs"],
                            "estimatedTimeToLearn": "2-4 weeks",
                            "category": "learn_new",
                            "relatedSkills": ["existing related skills"],
                            "difficultyLevel": "beginner|intermediate|advanced",
                            "careerImpact": "high|medium|low"
                        }
                    ],
                    "skillsToImprove": [
                        {
                            "skill": "existing skill to deepen",
                            "reason": "why go deeper",
                            "priority": 2,
                            "resources": ["advanced tutorials"],
                            "estimatedTimeToLearn": "1-2 weeks",
                            "category": "improve_existing",
                            "relatedSkills": [],
                            "difficultyLevel": "intermediate",
                            "careerImpact": "high"
                        }
                    ],
                    "practiceMore": [
                        {
                            "skill": "skill needing practice",
                            "reason": "why practice helps",
                            "priority": 3,
                            "resources": ["project ideas", "challenges"],
                            "estimatedTimeToLearn": "ongoing",
                            "category": "practice_more",
                            "relatedSkills": [],
                            "difficultyLevel": "intermediate",
                            "careerImpact": "medium"
                        }
                    ],
                    "engineeringHabitsToImprove": [
                        {
                            "habit": "testing|documentation|ci_cd|code_structure|error_handling",
                            "currentState": "description of current state based on metrics",
                            "targetState": "what good looks like",
                            "actionItems": ["specific steps to improve"],
                            "priority": 1
                        }
                    ],
                    "careerAdvice": ["specific career advice based on profile"],
                    "nextMilestone": "specific next career milestone"
                },
                "developerProfile": {
                    "experienceLevel": "Junior|Mid|Senior",
                    "primaryLanguages": ["top languages"],
                    "primaryFrameworks": ["top frameworks"],
                    "projectTypes": ["types of projects"],
                    "specialization": "backend|frontend|fullstack|data|devops|mobile",
                    "codingStyleAssessment": {
                        "style": "Engineering-Focused|Balanced|Rapid Prototyper",
                        "strengths": ["what they do well based on engineering metrics"],
                        "areasToImprove": ["specific areas from vibe analysis"],
                        "professionalFeedback": "2-3 sentence professional assessment of their development approach"
                    },
                    "skillDistribution": {"languages": 30, "frameworks": 25, "tools": 20, "concepts": 25},
                    "strengths": ["list of key strengths"],
                    "areasForGrowth": ["areas needing attention"],
                    "careerStage": "entry|junior|mid|senior|lead"
                }
            }
            
            Base everything on the provided pre-analyzed data. Be specific and actionable.
            For coding style feedback, be CONSTRUCTIVE and PROFESSIONAL - focus on growth, not criticism.
            """, profileJson);
        
        String response = callOpenAI(prompt, 3000);
        
        try {
            JsonNode json = objectMapper.readTree(response);
            
            // Parse skill analysis with enhanced fields
            JsonNode skillNode = json.get("skillAnalysis");
            int totalSkillsCount = skillNode.has("totalSkillsCount") ? skillNode.get("totalSkillsCount").asInt() : 0;
            
            AnalysisResult.SkillAnalysis skillAnalysis = AnalysisResult.SkillAnalysis.builder()
                .strongSkills(parseSkillsEnhanced(skillNode.get("strongSkills")))
                .moderateSkills(parseSkillsEnhanced(skillNode.get("moderateSkills")))
                .weakSkills(parseSkillsEnhanced(skillNode.get("weakSkills")))
                .missingSkills(jsonArrayToList(skillNode.get("missingSkills")))
                .totalSkillsCount(totalSkillsCount)
                .build();
            
            // Parse enhanced recommendations
            AnalysisResult.EnhancedRecommendations enhancedRecommendations = null;
            if (json.has("enhancedRecommendations")) {
                JsonNode enhNode = json.get("enhancedRecommendations");
                enhancedRecommendations = AnalysisResult.EnhancedRecommendations.builder()
                    .skillsToLearn(parseEnhancedRecommendations(enhNode.get("skillsToLearn")))
                    .skillsToImprove(parseEnhancedRecommendations(enhNode.get("skillsToImprove")))
                    .practiceMore(parseEnhancedRecommendations(enhNode.get("practiceMore")))
                    .engineeringHabitsToImprove(parseEngineeringHabits(enhNode.get("engineeringHabitsToImprove")))
                    .careerAdvice(jsonArrayToList(enhNode.get("careerAdvice")))
                    .nextMilestone(enhNode.has("nextMilestone") ? enhNode.get("nextMilestone").asText() : "")
                    .build();
            }
            
            // Build combined recommendations list for backward compatibility
            List<AnalysisResult.LearningRecommendation> allRecommendations = new ArrayList<>();
            if (enhancedRecommendations != null) {
                allRecommendations.addAll(enhancedRecommendations.getSkillsToLearn());
                allRecommendations.addAll(enhancedRecommendations.getSkillsToImprove());
                allRecommendations.addAll(enhancedRecommendations.getPracticeMore());
            }
            
            // Parse developer profile with enhanced fields including coding style assessment
            JsonNode profileNode = json.get("developerProfile");
            Map<String, Integer> skillDist = new HashMap<>();
            if (profileNode.has("skillDistribution")) {
                JsonNode distNode = profileNode.get("skillDistribution");
                distNode.fields().forEachRemaining(entry -> 
                    skillDist.put(entry.getKey(), entry.getValue().asInt()));
            }
            
            // Parse coding style assessment
            AnalysisResult.CodingStyleAssessment codingStyleAssessment = null;
            if (profileNode.has("codingStyleAssessment")) {
                JsonNode styleNode = profileNode.get("codingStyleAssessment");
                codingStyleAssessment = AnalysisResult.CodingStyleAssessment.builder()
                    .style(styleNode.has("style") ? styleNode.get("style").asText() : "Balanced")
                    .strengths(jsonArrayToList(styleNode.get("strengths")))
                    .areasToImprove(jsonArrayToList(styleNode.get("areasToImprove")))
                    .professionalFeedback(styleNode.has("professionalFeedback") ? styleNode.get("professionalFeedback").asText() : "")
                    .build();
            }
            
            AnalysisResult.DeveloperProfile developerProfile = AnalysisResult.DeveloperProfile.builder()
                .experienceLevel(profileNode.has("experienceLevel") ? profileNode.get("experienceLevel").asText() : "Junior")
                .primaryLanguages(jsonArrayToList(profileNode.get("primaryLanguages")))
                .primaryFrameworks(jsonArrayToList(profileNode.get("primaryFrameworks")))
                .projectTypes(jsonArrayToList(profileNode.get("projectTypes")))
                .specialization(profileNode.has("specialization") ? profileNode.get("specialization").asText() : "fullstack")
                .codingStyleAssessment(codingStyleAssessment)
                .skillDistribution(skillDist)
                .strengths(jsonArrayToList(profileNode.get("strengths")))
                .areasForGrowth(jsonArrayToList(profileNode.get("areasForGrowth")))
                .careerStage(profileNode.has("careerStage") ? profileNode.get("careerStage").asText() : "junior")
                .build();
            
            return AnalysisResult.builder()
                .userId(profile.getUserId())
                .skillAnalysis(skillAnalysis)
                .recommendations(allRecommendations)
                .enhancedRecommendations(enhancedRecommendations)
                .developerProfile(developerProfile)
                .totalRepositoriesAnalyzed(profile.getTotalProjects())
                .build();
                
        } catch (Exception e) {
            log.error("Failed to parse AI analysis response: {}", e.getMessage());
            throw new RuntimeException("Failed to analyze profile", e);
        }
    }
    
    private List<AnalysisResult.Skill> parseSkillsEnhanced(JsonNode skillsNode) {
        List<AnalysisResult.Skill> skills = new ArrayList<>();
        if (skillsNode != null && skillsNode.isArray()) {
            for (JsonNode node : skillsNode) {
                skills.add(AnalysisResult.Skill.builder()
                    .name(node.get("name").asText())
                    .category(node.has("category") ? node.get("category").asText() : "unknown")
                    .proficiencyScore(node.has("proficiencyScore") ? node.get("proficiencyScore").asInt() : 50)
                    .evidence(node.has("evidence") ? node.get("evidence").asText() : "")
                    .projectCount(node.has("projectCount") ? node.get("projectCount").asInt() : 1)
                    .trend(node.has("trend") ? node.get("trend").asText() : "stable")
                    .build());
            }
        }
        return skills;
    }
    
    private List<AnalysisResult.LearningRecommendation> parseEnhancedRecommendations(JsonNode recsNode) {
        List<AnalysisResult.LearningRecommendation> recs = new ArrayList<>();
        if (recsNode != null && recsNode.isArray()) {
            for (JsonNode node : recsNode) {
                recs.add(AnalysisResult.LearningRecommendation.builder()
                    .skill(node.get("skill").asText())
                    .reason(node.has("reason") ? node.get("reason").asText() : "")
                    .priority(node.has("priority") ? node.get("priority").asInt() : 3)
                    .resources(jsonArrayToList(node.get("resources")))
                    .estimatedTimeToLearn(node.has("estimatedTimeToLearn") ? node.get("estimatedTimeToLearn").asText() : "Unknown")
                    .category(node.has("category") ? node.get("category").asText() : "learn_new")
                    .relatedSkills(jsonArrayToList(node.get("relatedSkills")))
                    .difficultyLevel(node.has("difficultyLevel") ? node.get("difficultyLevel").asText() : "intermediate")
                    .careerImpact(node.has("careerImpact") ? node.get("careerImpact").asText() : "medium")
                    .build());
            }
        }
        return recs;
    }
    
    private List<AnalysisResult.EngineeringHabitRecommendation> parseEngineeringHabits(JsonNode habitsNode) {
        List<AnalysisResult.EngineeringHabitRecommendation> habits = new ArrayList<>();
        if (habitsNode != null && habitsNode.isArray()) {
            for (JsonNode node : habitsNode) {
                habits.add(AnalysisResult.EngineeringHabitRecommendation.builder()
                    .habit(node.has("habit") ? node.get("habit").asText() : "unknown")
                    .currentState(node.has("currentState") ? node.get("currentState").asText() : "")
                    .targetState(node.has("targetState") ? node.get("targetState").asText() : "")
                    .actionItems(jsonArrayToList(node.get("actionItems")))
                    .priority(node.has("priority") ? node.get("priority").asInt() : 3)
                    .build());
            }
        }
        return habits;
    }
    
    private String callOpenAI(String prompt, int maxTokens) {
        return callOpenAIWithRetry(prompt, maxTokens, 3); // 3 retries
    }
    
    private String callOpenAIWithRetry(String prompt, int maxTokens, int maxRetries) {
        Map<String, Object> requestBody = Map.of(
            "model", openAIConfig.getModel(),
            "messages", List.of(
                Map.of("role", "system", "content", "You are a helpful assistant that responds only with valid JSON."),
                Map.of("role", "user", "content", prompt)
            ),
            "max_tokens", maxTokens,
            "temperature", openAIConfig.getTemperature()
        );
        
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    // Exponential backoff: 2s, 4s, 8s
                    long waitTime = (long) Math.pow(2, attempt) * 1000;
                    log.info("Rate limited. Waiting {}ms before retry attempt {}/{}", waitTime, attempt, maxRetries);
                    Thread.sleep(waitTime);
                }
                
                JsonNode response = openAIWebClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + openAIConfig.getApiKey())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
                
                if (response == null || !response.has("choices")) {
                    throw new RuntimeException("Invalid response from OpenAI");
                }
                
                String content = response.get("choices").get(0).get("message").get("content").asText();
                
                // Extract JSON from response (in case there's extra text)
                int jsonStart = content.indexOf('{');
                int jsonEnd = content.lastIndexOf('}');
                if (jsonStart != -1 && jsonEnd != -1) {
                    content = content.substring(jsonStart, jsonEnd + 1);
                }
                
                return content;
                
            } catch (Exception e) {
                lastException = e;
                String message = e.getMessage() != null ? e.getMessage() : "";
                
                // Only retry on rate limit errors (429)
                if (message.contains("429") || message.contains("Too Many Requests")) {
                    log.warn("Rate limit hit on attempt {}/{}: {}", attempt + 1, maxRetries + 1, message);
                    if (attempt == maxRetries) {
                        throw new RuntimeException("OpenAI rate limit exceeded after " + (maxRetries + 1) + " attempts. Please wait a few minutes and try again.", e);
                    }
                    continue;
                }
                
                // For other errors, don't retry
                throw new RuntimeException("OpenAI API error: " + message, e);
            }
        }
        
        throw new RuntimeException("Failed to call OpenAI after retries", lastException);
    }
    
    private List<String> jsonArrayToList(JsonNode arrayNode) {
        List<String> list = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode node : arrayNode) {
                list.add(node.asText());
            }
        }
        return list;
    }
    
    private RepoSummary createMinimalSummary(String repoName, String language) {
        return RepoSummary.builder()
            .repoName(repoName)
            .technologies(language != null ? List.of(language) : List.of())
            .frameworks(List.of())
            .features(List.of())
            .projectType("Unknown")
            .complexity(RepoSummary.ComplexityLevel.BEGINNER)
            .description("No README available")
            .keywords(List.of())
            .build();
    }
    
    private RepoSummary.ComplexityLevel parseComplexity(String level) {
        try {
            return RepoSummary.ComplexityLevel.valueOf(level.toUpperCase());
        } catch (Exception e) {
            return RepoSummary.ComplexityLevel.INTERMEDIATE;
        }
    }
}
