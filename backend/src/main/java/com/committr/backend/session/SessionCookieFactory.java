package com.committr.backend.session;

import com.committr.backend.config.SessionProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class SessionCookieFactory {

    private final SessionProperties sessionProperties;

    public SessionCookieFactory(SessionProperties sessionProperties) {
        this.sessionProperties = sessionProperties;
    }

    public ResponseCookie createSessionCookie(String sessionId) {
        Duration maxAge = sessionProperties.ttl();
        return ResponseCookie.from(sessionProperties.cookieName(), sessionId)
            .httpOnly(true)
            .path("/")
            .maxAge(maxAge)
            .sameSite(sessionProperties.cookieSameSite())
            .secure(sessionProperties.cookieSecure())
            .build();
    }

    public ResponseCookie createDeleteCookie() {
        return ResponseCookie.from(sessionProperties.cookieName(), "")
            .httpOnly(true)
            .path("/")
            .sameSite(sessionProperties.cookieSameSite())
            .secure(sessionProperties.cookieSecure())
            .maxAge(0)
            .build();
    }
}
