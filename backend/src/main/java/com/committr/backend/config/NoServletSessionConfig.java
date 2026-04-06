package com.committr.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class NoServletSessionConfig {

    @Bean
    public FilterRegistrationBean<NoServletSessionFilter> noServletSessionFilterRegistration() {
        FilterRegistrationBean<NoServletSessionFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new NoServletSessionFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.addUrlPatterns("/*");
        return bean;
    }

    static final class NoServletSessionFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
            filterChain.doFilter(wrap(request), response);
        }

        private static HttpServletRequest wrap(HttpServletRequest request) {
            return new HttpServletRequestWrapper(request) {
                @Override
                public HttpSession getSession(boolean create) {
                    return super.getSession(false);
                }

                @Override
                public HttpSession getSession() {
                    return super.getSession(false);
                }
            };
        }
    }
}
