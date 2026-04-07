package com.committr.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryRepository extends JpaRepository<RepositoryEntity, Long> {

    List<RepositoryEntity> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<RepositoryEntity> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndGithubRepoId(Long userId, Long githubRepoId);
}
