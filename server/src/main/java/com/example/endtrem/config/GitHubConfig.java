package com.example.endtrem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "github")
public class GitHubConfig {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scope = "read:user user:email repo";
}
