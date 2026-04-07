package com.committr.backend.dto.repository;

import com.committr.backend.repository.RepositoryEntity;
import java.time.LocalDateTime;

public record RepositoryResponse(
    Long id,
    Long githubRepoId,
    String name,
    String fullName,
    String ownerLogin,
    boolean isPrivate,
    String htmlUrl,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static RepositoryResponse from(RepositoryEntity e) {
        return new RepositoryResponse(
            e.getId(),
            e.getGithubRepoId(),
            e.getName(),
            e.getFullName(),
            e.getOwnerLogin(),
            e.isPrivate(),
            e.getHtmlUrl(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
