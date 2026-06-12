package com.committr.backend.github;

import com.committr.backend.config.GitHubOAuthProperties;
import com.committr.backend.dto.github.CommitSummary;
import com.committr.backend.dto.github.ContributorSummary;
import com.committr.backend.dto.github.GitHubRepoResponse;
import com.committr.backend.dto.github.PullRequestSummary;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.ParameterizedTypeReference;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);
    private static final String BASE = "https://api.github.com";
    private static final String REPO_URL_TEMPLATE = BASE + "/repos/{owner}/{repo}";
    private static final Pattern NEXT_LINK = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
    private static final int RATE_LIMIT_GUARD = 10;
    private static final int MAX_PAGES = 100;

    private static final Map<String, String> EXT_TO_LANGUAGE = Map.ofEntries(
        Map.entry("java",   "Java"),
        Map.entry("kt",     "Kotlin"),
        Map.entry("ts",     "TypeScript"),
        Map.entry("tsx",    "TypeScript"),
        Map.entry("js",     "JavaScript"),
        Map.entry("jsx",    "JavaScript"),
        Map.entry("py",     "Python"),
        Map.entry("go",     "Go"),
        Map.entry("rs",     "Rust"),
        Map.entry("css",    "CSS"),
        Map.entry("html",   "HTML"),
        Map.entry("htm",    "HTML"),
        Map.entry("sql",    "SQL"),
        Map.entry("yaml",   "YAML"),
        Map.entry("yml",    "YAML"),
        Map.entry("json",   "JSON"),
        Map.entry("md",     "Markdown")
    );

    private final RestTemplate restTemplate;
    private final GitHubOAuthProperties oauthProperties;

    public GitHubClient(RestTemplate restTemplate, GitHubOAuthProperties oauthProperties) {
        this.restTemplate = restTemplate;
        this.oauthProperties = oauthProperties;
    }

    public GitHubRepoResponse getRepository(String owner, String repo) {
        long startNanos = System.nanoTime();
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
            log.info(
                "GitHub GET /repos/{}/{} completed in {} ms",
                owner,
                repo,
                (System.nanoTime() - startNanos) / 1_000_000
            );
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

    public List<ContributorSummary> getContributors(String owner, String repo, String accessToken) {
        String url = BASE + "/repos/" + owner + "/" + repo + "/contributors?per_page=100&anon=0";
        List<RawContributor> raw = fetchAllPages(url, accessToken,
                new ParameterizedTypeReference<List<RawContributor>>() {});
        return raw.stream()
                .map(c -> new ContributorSummary(c.login, c.avatarUrl, c.contributions))
                .toList();
    }

    public List<PullRequestSummary> getMergedPullRequests(String owner, String repo, String accessToken) {
        String url = BASE + "/repos/" + owner + "/" + repo + "/pulls?state=closed&per_page=100";
        List<RawPullRequest> raw = fetchAllPages(url, accessToken,
                new ParameterizedTypeReference<List<RawPullRequest>>() {});
        return raw.stream()
                .filter(pr -> pr.mergedAt != null)
                .map(pr -> new PullRequestSummary(pr.number, pr.createdAt, pr.mergedAt))
                .toList();
    }

    public List<CommitSummary> getCommits(String owner, String repo, String accessToken) {
        String url = BASE + "/repos/" + owner + "/" + repo + "/commits?per_page=100";
        List<RawCommitListItem> items = fetchAllPages(url, accessToken,
                new ParameterizedTypeReference<List<RawCommitListItem>>() {});

        List<CommitSummary> results = new ArrayList<>(items.size());
        for (RawCommitListItem item : items) {
            RawCommitDetail detail = getCommitDetail(owner, repo, item.sha, accessToken);
            int added = detail.stats != null ? detail.stats.additions : 0;
            int deleted = detail.stats != null ? detail.stats.deletions : 0;
            String language = dominantLanguage(detail.files);

            String login = item.author != null ? item.author.login : null;
            if (login == null && item.commit != null && item.commit.author != null) {
                login = item.commit.author.name;
            }
            String avatarUrl = item.author != null ? item.author.avatarUrl : null;
            Instant committedAt = item.commit != null && item.commit.author != null
                    ? item.commit.author.date : null;

            results.add(new CommitSummary(item.sha, login, avatarUrl, committedAt,
                    item.commit != null ? item.commit.message : null, added, deleted, language));
        }
        return results;
    }

    private RawCommitDetail getCommitDetail(String owner, String repo, String sha, String accessToken) {
        String url = BASE + "/repos/" + owner + "/" + repo + "/commits/" + sha;
        HttpHeaders headers = authHeaders(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<RawCommitDetail> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, RawCommitDetail.class);
            return response.getBody() != null ? response.getBody() : new RawCommitDetail();
        } catch (HttpClientErrorException ex) {
            log.warn("Could not fetch commit detail for sha {}: {}", sha, ex.getStatusCode());
            return new RawCommitDetail();
        }
    }

    private <T> List<T> fetchAllPages(String firstUrl, String accessToken,
                                       ParameterizedTypeReference<List<T>> type) {
        List<T> all = new ArrayList<>();
        String url = firstUrl;
        int page = 0;
        while (url != null && page++ < MAX_PAGES) {
            HttpHeaders headers = authHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<List<T>> response;
            try {
                response = restTemplate.exchange(url, HttpMethod.GET, request, type);
            } catch (HttpClientErrorException.Unauthorized ex) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid GitHub token.", ex);
            } catch (HttpClientErrorException.NotFound ex) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "GitHub resource not found.", ex);
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode().value() == 403) {
                    throw new RateLimitException("GitHub rate limit exceeded.");
                }
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "GitHub API error.", ex);
            }
            checkRateLimit(response.getHeaders());
            List<T> body = response.getBody();
            if (body != null) all.addAll(body);
            url = extractNextUrl(response.getHeaders());
        }
        return all;
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (oauthProperties.userAgent() != null && !oauthProperties.userAgent().isBlank()) {
            headers.set(HttpHeaders.USER_AGENT, oauthProperties.userAgent());
        }
        if (accessToken != null && !accessToken.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return headers;
    }

    private void checkRateLimit(HttpHeaders headers) {
        String remaining = headers.getFirst("X-RateLimit-Remaining");
        if (remaining != null) {
            try {
                if (Integer.parseInt(remaining) < RATE_LIMIT_GUARD) {
                    throw new RateLimitException("GitHub rate limit almost exhausted (" + remaining + " remaining).");
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private String extractNextUrl(HttpHeaders headers) {
        String link = headers.getFirst("Link");
        if (link == null) return null;
        Matcher m = NEXT_LINK.matcher(link);
        return m.find() ? m.group(1) : null;
    }

    private String dominantLanguage(List<RawFile> files) {
        if (files == null || files.isEmpty()) return null;
        Map<String, Long> counts = new java.util.HashMap<>();
        for (RawFile f : files) {
            if (f.filename == null) continue;
            int dot = f.filename.lastIndexOf('.');
            if (dot < 0) continue;
            String ext = f.filename.substring(dot + 1).toLowerCase();
            String lang = EXT_TO_LANGUAGE.get(ext);
            if (lang != null) counts.merge(lang, 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // --- raw GitHub API response shapes ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RawContributor {
        public String login;
        @JsonProperty("avatar_url") public String avatarUrl;
        public int contributions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RawPullRequest {
        public int number;
        @JsonProperty("created_at") public Instant createdAt;
        @JsonProperty("merged_at")  public Instant mergedAt;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RawCommitListItem {
        public String sha;
        public RawAuthorRef author;
        public RawCommitWrapper commit;

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class RawAuthorRef {
            public String login;
            @JsonProperty("avatar_url") public String avatarUrl;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class RawCommitWrapper {
            public String message;
            public RawCommitAuthor author;

            @JsonIgnoreProperties(ignoreUnknown = true)
            static class RawCommitAuthor {
                public String name;
                public Instant date;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RawCommitDetail {
        public RawStats stats;
        public List<RawFile> files = Collections.emptyList();

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class RawStats {
            public int additions;
            public int deletions;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RawFile {
        public String filename;
    }
}
