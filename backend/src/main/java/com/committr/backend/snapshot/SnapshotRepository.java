package com.committr.backend.snapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotRepository extends JpaRepository<SnapshotEntity, Long> {

    Optional<SnapshotEntity> findByRepositoryIdAndContributorIdAndWeekStart(
            Long repositoryId, Long contributorId, LocalDate weekStart);

    List<SnapshotEntity> findByRepositoryIdOrderByWeekStartAsc(Long repositoryId);
}
