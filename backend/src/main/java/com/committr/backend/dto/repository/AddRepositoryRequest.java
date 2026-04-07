package com.committr.backend.dto.repository;

import jakarta.validation.constraints.NotBlank;

public record AddRepositoryRequest(@NotBlank String fullName) {}
