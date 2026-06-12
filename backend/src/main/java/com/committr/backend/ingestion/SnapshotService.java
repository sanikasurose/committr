package com.committr.backend.ingestion;

import com.committr.backend.commit.CommitEntity;
import com.committr.backend.commit.CommitRepository;
import com.committr.backend.contributor.ContributorEntity;
import com.committr.backend.prEvent.PrEventEntity;
import com.committr.backend.prEvent.PrEventRepository;
import com.committr.backend.repository.RepositoryEntity;
import com.committr.backend.repository.RepositoryRepository;
import com.committr.backend.snapshot.SnapshotEntity;
import com.committr.backend.snapshot.SnapshotRepository;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SnapshotService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);

    private final RepositoryRepository repositoryRepository;
    private final CommitRepository commitRepository;
    private final PrEventRepository prEventRepository;
    private final SnapshotRepository snapshotRepository;

    public SnapshotService(
        RepositoryRepository repositoryRepository,
        CommitRepository commitRepository,
        PrEventRepository prEventRepository,
        SnapshotRepository snapshotRepository
    ) {
        this.repositoryRepository = repositoryRepository;
        this.commitRepository = commitRepository;
        this.prEventRepository = prEventRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public void buildSnapshots(Long repositoryId) {
        RepositoryEntity repo = repositoryRepository.findById(repositoryId)
            .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repositoryId));

        List<CommitEntity> commits = commitRepository.findByRepositoryIdOrderByCommittedAtDesc(repositoryId);
        List<PrEventEntity> prEvents = prEventRepository
            .findByRepositoryIdAndMergedAtIsNotNullOrderByMergedAtAsc(repositoryId);

        // key: contributorId + weekStart
        Map<String, SnapshotAccumulator> accumulators = new HashMap<>();

        for (CommitEntity commit : commits) {
            if (commit.getCommittedAt() == null) continue;
            LocalDate weekStart = toWeekStart(commit.getCommittedAt().toLocalDate());
            String key = commit.getContributor().getId() + ":" + weekStart;

            accumulators.computeIfAbsent(key, k ->
                new SnapshotAccumulator(commit.getContributor(), weekStart))
                .addCommit(commit);
        }

        // distribute PR merge times equally across all contributors active that week
        for (PrEventEntity pr : prEvents) {
            if (pr.getMergedAt() == null || pr.getOpenedAt() == null) continue;
            Duration mergeTime = Duration.between(pr.getOpenedAt(), pr.getMergedAt());
            LocalDate weekStart = toWeekStart(pr.getMergedAt().toLocalDate());

            // find contributors with commits that week and split the PR merge time among them
            List<SnapshotAccumulator> activeThisWeek = accumulators.values().stream()
                .filter(a -> a.weekStart.equals(weekStart))
                .toList();

            if (!activeThisWeek.isEmpty()) {
                Duration perContributor = mergeTime.dividedBy(activeThisWeek.size());
                for (SnapshotAccumulator a : activeThisWeek) {
                    a.addPrMergeTime(perContributor);
                }
            }
        }

        // upsert snapshots
        int upserted = 0;
        for (SnapshotAccumulator acc : accumulators.values()) {
            SnapshotEntity snapshot = snapshotRepository
                .findByRepositoryIdAndContributorIdAndWeekStart(
                    repositoryId, acc.contributor.getId(), acc.weekStart)
                .orElseGet(() -> {
                    SnapshotEntity s = new SnapshotEntity();
                    s.setRepository(repo);
                    s.setContributor(acc.contributor);
                    s.setWeekStart(acc.weekStart);
                    return s;
                });

            snapshot.setCommitCount(acc.commitCount);
            snapshot.setLinesAdded(acc.linesAdded);
            snapshot.setLinesDeleted(acc.linesDeleted);
            snapshot.setPrMergeTime(acc.totalPrMergeTime);
            snapshot.setLanguageDistribution(acc.languageCounts.isEmpty() ? null : acc.languageCounts);
            snapshot.setCodingHours(acc.codingHours.stream().anyMatch(v -> v > 0) ? acc.codingHours : null);
            snapshotRepository.save(snapshot);
            upserted++;
        }

        log.debug("Upserted {} snapshots for repo {}", upserted, repositoryId);
    }

    private static LocalDate toWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static class SnapshotAccumulator {
        final ContributorEntity contributor;
        final LocalDate weekStart;
        int commitCount = 0;
        int linesAdded = 0;
        int linesDeleted = 0;
        Duration totalPrMergeTime = null;
        final Map<String, Integer> languageCounts = new HashMap<>();
        final List<Integer> codingHours = new ArrayList<>(java.util.Collections.nCopies(24, 0));

        SnapshotAccumulator(ContributorEntity contributor, LocalDate weekStart) {
            this.contributor = contributor;
            this.weekStart = weekStart;
        }

        void addCommit(CommitEntity commit) {
            commitCount++;
            linesAdded += commit.getLinesAdded();
            linesDeleted += commit.getLinesDeleted();
            if (commit.getLanguage() != null) {
                languageCounts.merge(commit.getLanguage(), 1, Integer::sum);
            }
            if (commit.getCommittedAt() != null) {
                int hour = commit.getCommittedAt().getHour();
                codingHours.set(hour, codingHours.get(hour) + 1);
            }
        }

        void addPrMergeTime(Duration duration) {
            totalPrMergeTime = totalPrMergeTime == null ? duration : totalPrMergeTime.plus(duration);
        }
    }
}
