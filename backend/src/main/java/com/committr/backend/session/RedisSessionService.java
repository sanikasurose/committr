package com.committr.backend.session;

import com.committr.backend.config.SessionProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisSessionService {

    private static final Logger log = LoggerFactory.getLogger(RedisSessionService.class);

    private static final String KEY_PREFIX = "session:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SessionProperties sessionProperties;

    public RedisSessionService(
        StringRedisTemplate stringRedisTemplate,
        ObjectMapper objectMapper,
        SessionProperties sessionProperties
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.sessionProperties = sessionProperties;
    }

    public String createSession(SessionUserDto user) {
        String sessionId = UUID.randomUUID().toString();
        System.out.println("CREATING SESSION: " + sessionId);
        try {
            String json = objectMapper.writeValueAsString(user);
            System.out.println("Saving to Redis...");
            stringRedisTemplate.opsForValue().set(redisKey(sessionId), json, sessionProperties.ttl());
            log.info("Redis session stored key={} ttl={}", redisKey(sessionId), sessionProperties.ttl());
            return sessionId;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize session payload", e);
        }
    }

    public Optional<SessionUserDto> getSessionAndRefreshTtl(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        String key = redisKey(sessionId);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        stringRedisTemplate.expire(key, sessionProperties.ttl());
        try {
            return Optional.of(objectMapper.readValue(json, SessionUserDto.class));
        } catch (JsonProcessingException e) {
            stringRedisTemplate.delete(key);
            return Optional.empty();
        }
    }

    public void deleteSession(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            stringRedisTemplate.delete(redisKey(sessionId));
        }
    }

    private static String redisKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
