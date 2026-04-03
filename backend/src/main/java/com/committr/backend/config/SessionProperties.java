package com.committr.backend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "committr.session")
public record SessionProperties(
    @DefaultValue("30m") Duration ttl,
    @DefaultValue("sessionId") String cookieName,
    @DefaultValue("false") boolean cookieSecure,
    @DefaultValue("Lax") String cookieSameSite
) {}
