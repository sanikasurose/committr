package com.committr.backend.dto.auth;

import com.committr.backend.dto.github.GithubUserLogPayload;
import com.committr.backend.session.SessionUserDto;

public record OAuthLoginCompletion(GithubUserLogPayload payload, SessionUserDto sessionUser) {}
