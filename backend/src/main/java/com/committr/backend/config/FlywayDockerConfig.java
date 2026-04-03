package com.committr.backend.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Docker Compose runs against a persisted Postgres volume; if a migration file is edited after it was
 * applied, Flyway validation fails. Repair resynchronizes checksums in {@code flyway_schema_history}.
 * Do not use the {@code docker} profile against production databases.
 */
@Configuration
@Profile("docker")
public class FlywayDockerConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
