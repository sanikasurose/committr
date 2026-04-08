package com.committr.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryRepository extends JpaRepository<RepositoryEntity, Long> {

    List<RepositoryEntity> findByUser_IdAndDeletedAtIsNull(Long userId);

    Optional<RepositoryEntity> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByUser_IdAndGithubRepoId(Long userId, Long githubRepoId);

    Optional<RepositoryEntity> findByUser_IdAndGithubRepoId(Long userId, Long githubRepoId);
}
