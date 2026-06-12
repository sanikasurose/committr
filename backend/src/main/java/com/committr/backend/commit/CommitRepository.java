package com.committr.backend.commit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommitRepository extends JpaRepository<CommitEntity, Long> {

    boolean existsByGithubCommitId(String githubCommitId);

    List<CommitEntity> findByRepositoryIdOrderByCommittedAtDesc(Long repositoryId);
}
