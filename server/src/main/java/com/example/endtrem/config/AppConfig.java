package com.example.endtrem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableMongoAuditing
public class AppConfig {
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    @Bean
    public WebClient gitHubWebClient(WebClient.Builder builder) {
        return builder
            .baseUrl("https://api.github.com")
            .defaultHeader("Accept", "application/vnd.github.v3+json")
            .build();
    }
    
    @Bean
    public WebClient openAIWebClient(WebClient.Builder builder, OpenAIConfig openAIConfig) {
        return builder
            .baseUrl(openAIConfig.getBaseUrl())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
}
