package com.committr.backend.session;

import com.committr.backend.config.SessionProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final RedisSessionService redisSessionService;
    private final SessionProperties sessionProperties;

    public SessionAuthenticationFilter(RedisSessionService redisSessionService, SessionProperties sessionProperties) {
        this.redisSessionService = redisSessionService;
        this.sessionProperties = sessionProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        var cookie = WebUtils.getCookie(request, sessionProperties.cookieName());
        if (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank()) {
            redisSessionService
                .getSessionAndRefreshTtl(cookie.getValue())
                .ifPresent(
                    user -> SecurityContextHolder.getContext()
                        .setAuthentication(new SessionAuthenticationToken(user))
                );
        }
        filterChain.doFilter(request, response);
    }
}
