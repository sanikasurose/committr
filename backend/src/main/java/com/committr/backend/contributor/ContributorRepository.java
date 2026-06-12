package com.committr.backend.contributor;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributorRepository extends JpaRepository<ContributorEntity, Long> {

    Optional<ContributorEntity> findByGithubLoginAndRepositoryId(String githubLogin, Long repositoryId);

    List<ContributorEntity> findByRepositoryId(Long repositoryId);
}
