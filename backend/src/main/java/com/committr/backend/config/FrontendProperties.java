package com.committr.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "committr.frontend")
public record FrontendProperties(
    @DefaultValue("http://localhost:4200") String url
) {
    /** Base URL without a trailing slash, safe to concatenate paths onto. */
    public String baseUrl() {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
