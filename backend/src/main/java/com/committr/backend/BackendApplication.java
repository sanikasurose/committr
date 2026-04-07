package com.committr.backend;

import com.committr.backend.config.CorsProperties;
import com.committr.backend.config.GitHubOAuthProperties;
import com.committr.backend.config.SecurityProperties;
import com.committr.backend.config.SessionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableConfigurationProperties({
    GitHubOAuthProperties.class,
    SecurityProperties.class,
    SessionProperties.class,
    CorsProperties.class
})
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
