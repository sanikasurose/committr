package com.committr.backend.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub may return either OAuth token fields or error fields
 * (<a href="https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps">docs</a>).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubAccessTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    String scope,
    String error,
    @JsonProperty("error_description") String errorDescription,
    @JsonProperty("error_uri") String errorUri
) {
}
