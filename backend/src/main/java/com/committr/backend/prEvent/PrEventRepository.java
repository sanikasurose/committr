package com.committr.backend.prEvent;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrEventRepository extends JpaRepository<PrEventEntity, Long> {

    boolean existsByRepositoryIdAndPrNumber(Long repositoryId, int prNumber);

    List<PrEventEntity> findByRepositoryIdAndMergedAtIsNotNullOrderByMergedAtAsc(Long repositoryId);
}
