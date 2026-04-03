package com.committr.backend.config;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    /**
     * Explicit template for diagnostics and optional startup smoke tests; session storage uses
     * {@link org.springframework.data.redis.core.StringRedisTemplate} from auto-configuration.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnProperty(name = "committr.session.redis-startup-test", havingValue = "true")
    public ApplicationListener<ApplicationReadyEvent> redisStartupTestKey(RedisTemplate<String, Object> redisTemplate) {
        return event -> {
            redisTemplate.opsForValue().set("test", "hello", Duration.ofMinutes(10));
            log.info("committr.session.redis-startup-test: wrote test=hello (TTL 10m); run KEYS * in redis-cli.");
        };
    }
}
