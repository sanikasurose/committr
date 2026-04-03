package com.committr.backend.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "committr.cors")
public record CorsProperties(List<String> allowedOriginPatterns) {

    public CorsProperties {
        if (allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()) {
            allowedOriginPatterns = List.of("http://localhost:*");
        }
    }
}
