package com.example.endtrem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret;
    private long expiration = 86400000; // 24 hours in milliseconds
    private long refreshExpiration = 604800000; // 7 days in milliseconds
}
