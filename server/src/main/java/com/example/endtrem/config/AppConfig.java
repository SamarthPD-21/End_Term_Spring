package com.example.endtrem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableMongoAuditing
public class AppConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024);
            })
            .build();
        
        return WebClient.builder().exchangeStrategies(strategies);
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
