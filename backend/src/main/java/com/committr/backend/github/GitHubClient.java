package com.committr.backend.github;

import com.committr.backend.config.GitHubOAuthProperties;
import com.committr.backend.dto.github.GitHubRepoResponse;
import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GitHubClient {

    private static final String REPO_URL_TEMPLATE = "https://api.github.com/repos/{owner}/{repo}";

    private final RestTemplate restTemplate;
    private final GitHubOAuthProperties oauthProperties;

    public GitHubClient(RestTemplate restTemplate, GitHubOAuthProperties oauthProperties) {
        this.restTemplate = restTemplate;
        this.oauthProperties = oauthProperties;
    }

    public GitHubRepoResponse getRepository(String owner, String repo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (oauthProperties.userAgent() != null && !oauthProperties.userAgent().isBlank()) {
            headers.set(HttpHeaders.USER_AGENT, oauthProperties.userAgent());
        }
        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<GitHubRepoResponse> response = restTemplate.exchange(
                REPO_URL_TEMPLATE,
                HttpMethod.GET,
                request,
                GitHubRepoResponse.class,
                owner,
                repo
            );
            GitHubRepoResponse body = response.getBody();
            if (body == null || body.id() == null) {
                throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "GitHub returned an invalid repository response."
                );
            }
            if (body.owner() == null || body.owner().login() == null || body.owner().login().isBlank()) {
                throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "GitHub repository response was missing owner information."
                );
            }
            if (body.name() == null || body.name().isBlank()
                || body.fullName() == null || body.fullName().isBlank()
                || body.htmlUrl() == null || body.htmlUrl().isBlank()) {
                throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "GitHub repository response was missing required fields."
                );
            }
            return body;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "GitHub repository not found.", ex);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "GitHub API request failed.",
                ex
            );
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "GitHub API is unreachable.", ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error calling GitHub API.", ex);
        }
    }
}
