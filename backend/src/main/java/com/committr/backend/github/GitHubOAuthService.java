package com.committr.backend.github;

import com.committr.backend.config.GitHubOAuthProperties;
import com.committr.backend.dto.auth.OAuthLoginCompletion;
import com.committr.backend.dto.github.GithubAccessTokenResponse;
import com.committr.backend.dto.github.GithubUserLogPayload;
import com.committr.backend.dto.github.GithubUserResponse;
import com.committr.backend.session.SessionUserDto;
import com.committr.backend.user.User;
import com.committr.backend.user.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GitHubOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GitHubOAuthService.class);

    private final RestTemplate restTemplate;
    private final GitHubOAuthProperties properties;
    private final ObjectMapper objectMapper;
    private final UserService userService;

    public GitHubOAuthService(
        RestTemplate restTemplate,
        GitHubOAuthProperties properties,
        ObjectMapper objectMapper,
        UserService userService
    ) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    public URI buildAuthorizationUri() {
        if (properties.clientId() == null || properties.clientId().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "GitHub OAuth is not configured: set github.oauth.client-id (e.g. GITHUB_CLIENT_ID)."
            );
        }
        String url = UriComponentsBuilder
            .fromUriString(properties.authorizeUrl())
            .queryParam("client_id", properties.clientId())
            .queryParam("redirect_uri", properties.redirectUri())
            .queryParam("scope", properties.scope())
            .encode(StandardCharsets.UTF_8)
            .build()
            .toUriString();
        return URI.create(url);
    }

    public OAuthLoginCompletion completeLogin(String authorizationCode) {
        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing authorization code.");
        }
        if (properties.clientSecret() == null || properties.clientSecret().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "GitHub OAuth client secret is not configured (github.oauth.client-secret)."
            );
        }

        String accessToken = exchangeCodeForAccessToken(authorizationCode);
        GithubUserResponse user = fetchGitHubUser(accessToken);
        if (user.id() == null || user.login() == null || user.login().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "GitHub user response was missing required fields."
            );
        }

        User saved = userService.upsertUser(user, accessToken);

        String avatarUrl = user.avatarUrl() == null ? "" : user.avatarUrl();
        GithubUserLogPayload payload = new GithubUserLogPayload(
            String.valueOf(user.id()),
            user.login(),
            avatarUrl
        );
        logUserPayload(payload);
        SessionUserDto sessionUser = new SessionUserDto(
            saved.getId(),
            saved.getUsername(),
            saved.getAvatarUrl() != null ? saved.getAvatarUrl() : ""
        );
        return new OAuthLoginCompletion(payload, sessionUser);
    }

    private String exchangeCodeForAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", properties.clientId());
        body.add("client_secret", properties.clientSecret());
        body.add("code", code);
        body.add("redirect_uri", properties.redirectUri());

        HttpHeaders headers = baseGithubHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<GithubAccessTokenResponse> response = restTemplate.exchange(
                properties.accessTokenUrl(),
                HttpMethod.POST,
                request,
                GithubAccessTokenResponse.class
            );
            GithubAccessTokenResponse token = response.getBody();
            if (token == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "GitHub returned an empty access token response."
                );
            }
            if (token.error() != null && !token.error().isBlank()) {
                String detail = token.error();
                if (token.errorDescription() != null && !token.errorDescription().isBlank()) {
                    detail = detail + ": " + token.errorDescription();
                }
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "GitHub OAuth token error: " + detail
                );
            }
            if (token.accessToken() == null || token.accessToken().isBlank()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "GitHub access token response missing access_token."
                );
            }
            return token.accessToken();
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Failed to exchange code with GitHub.",
                ex
            );
        }
    }

    private GithubUserResponse fetchGitHubUser(String accessToken) {
        HttpHeaders headers = baseGithubHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<GithubUserResponse> response = restTemplate.exchange(
                properties.userApiUrl(),
                HttpMethod.GET,
                request,
                GithubUserResponse.class
            );
            GithubUserResponse user = response.getBody();
            if (user == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "GitHub returned an empty user profile."
                );
            }
            return user;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Failed to fetch GitHub user profile.",
                ex
            );
        }
    }

    private HttpHeaders baseGithubHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (properties.userAgent() != null && !properties.userAgent().isBlank()) {
            headers.set(HttpHeaders.USER_AGENT, properties.userAgent());
        }
        return headers;
    }

    private void logUserPayload(GithubUserLogPayload payload) {
        try {
            log.info(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize GitHub user log payload", ex);
        }
    }
}
