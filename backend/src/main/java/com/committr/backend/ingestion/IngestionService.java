package com.committr.backend.ingestion;

import com.committr.backend.commit.CommitEntity;
import com.committr.backend.commit.CommitRepository;
import com.committr.backend.contributor.ContributorEntity;
import com.committr.backend.contributor.ContributorRepository;
import com.committr.backend.crypto.EncryptionService;
import com.committr.backend.dto.github.CommitSummary;
import com.committr.backend.dto.github.ContributorSummary;
import com.committr.backend.dto.github.PullRequestSummary;
import com.committr.backend.github.GitHubClient;
import com.committr.backend.github.RateLimitException;
import com.committr.backend.prEvent.PrEventEntity;
import com.committr.backend.prEvent.PrEventRepository;
import com.committr.backend.repository.RepositoryEntity;
import com.committr.backend.repository.RepositoryRepository;
import com.committr.backend.user.User;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final GitHubClient gitHubClient;
    private final EncryptionService encryptionService;
    private final RepositoryRepository repositoryRepository;
    private final ContributorRepository contributorRepository;
    private final CommitRepository commitRepository;
    private final PrEventRepository prEventRepository;
    private final SnapshotService snapshotService;
    private final StringRedisTemplate redisTemplate;

    public IngestionService(
        GitHubClient gitHubClient,
        EncryptionService encryptionService,
        RepositoryRepository repositoryRepository,
        ContributorRepository contributorRepository,
        CommitRepository commitRepository,
        PrEventRepository prEventRepository,
        SnapshotService snapshotService,
        StringRedisTemplate redisTemplate
    ) {
        this.gitHubClient = gitHubClient;
        this.encryptionService = encryptionService;
        this.repositoryRepository = repositoryRepository;
        this.contributorRepository = contributorRepository;
        this.commitRepository = commitRepository;
        this.prEventRepository = prEventRepository;
        this.snapshotService = snapshotService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void ingest(Long repositoryId) {
        RepositoryEntity repo = repositoryRepository.findById(repositoryId)
            .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repositoryId));

        User owner = repo.getUser();
        String token = encryptionService.decrypt(owner.getEncryptedAccessToken());

        String repoOwner = repo.getOwnerLogin();
        String repoName = repo.getName();

        log.info("Starting ingestion for {}/{} (repoId={})", repoOwner, repoName, repositoryId);

        try {
            ingestContributors(repo, repoOwner, repoName, token);
            ingestCommits(repo, repoOwner, repoName, token);
            ingestPrEvents(repo, repoOwner, repoName, token);
            snapshotService.buildSnapshots(repositoryId);
            evictCacheForRepo(repositoryId);
            log.info("Ingestion complete for {}/{}", repoOwner, repoName);
        } catch (RateLimitException ex) {
            log.warn("GitHub rate limit hit during ingestion of {}/{} — will retry next cycle", repoOwner, repoName);
        }
    }

    private void ingestContributors(RepositoryEntity repo, String owner, String name, String token) {
        List<ContributorSummary> summaries = gitHubClient.getContributors(owner, name, token);
        for (ContributorSummary s : summaries) {
            contributorRepository.findByGithubLoginAndRepositoryId(s.login(), repo.getId())
                .ifPresentOrElse(
                    existing -> {
                        existing.setTotalCommits(s.contributions());
                        existing.setGithubAvatarUrl(s.avatarUrl());
                        contributorRepository.save(existing);
                    },
                    () -> {
                        ContributorEntity c = new ContributorEntity();
                        c.setRepository(repo);
                        c.setGithubLogin(s.login());
                        c.setGithubAvatarUrl(s.avatarUrl());
                        c.setTotalCommits(s.contributions());
                        contributorRepository.save(c);
                    }
                );
        }
        log.debug("Upserted {} contributors for repo {}", summaries.size(), repo.getId());
    }

    private void ingestCommits(RepositoryEntity repo, String owner, String name, String token) {
        List<CommitSummary> summaries = gitHubClient.getCommits(owner, name, token);
        int newCount = 0;
        for (CommitSummary s : summaries) {
            if (commitRepository.existsByGithubCommitId(s.sha())) {
                continue;
            }
            ContributorEntity contributor = resolveContributor(repo, s.authorLogin(), s.authorAvatarUrl());

            CommitEntity commit = new CommitEntity();
            commit.setRepository(repo);
            commit.setContributor(contributor);
            commit.setGithubCommitId(s.sha());
            commit.setMessage(s.message());
            commit.setCommittedAt(s.committedAt() != null
                ? LocalDateTime.ofInstant(s.committedAt(), ZoneOffset.UTC) : null);
            commit.setLinesAdded(s.linesAdded());
            commit.setLinesDeleted(s.linesDeleted());
            commit.setLanguage(s.language());
            commitRepository.save(commit);
            newCount++;
        }
        log.debug("Inserted {} new commits for repo {}", newCount, repo.getId());
    }

    private void ingestPrEvents(RepositoryEntity repo, String owner, String name, String token) {
        List<PullRequestSummary> summaries = gitHubClient.getMergedPullRequests(owner, name, token);
        int newCount = 0;
        for (PullRequestSummary s : summaries) {
            if (prEventRepository.existsByRepositoryIdAndPrNumber(repo.getId(), s.number())) {
                continue;
            }
            PrEventEntity pr = new PrEventEntity();
            pr.setRepository(repo);
            pr.setPrNumber(s.number());
            pr.setOpenedAt(s.openedAt() != null
                ? LocalDateTime.ofInstant(s.openedAt(), ZoneOffset.UTC) : null);
            pr.setMergedAt(s.mergedAt() != null
                ? LocalDateTime.ofInstant(s.mergedAt(), ZoneOffset.UTC) : null);
            prEventRepository.save(pr);
            newCount++;
        }
        log.debug("Inserted {} new PR events for repo {}", newCount, repo.getId());
    }

    private void evictCacheForRepo(Long repositoryId) {
        Set<String> keys = redisTemplate.keys("*:" + repositoryId + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Evicted {} cache keys for repo {}", keys.size(), repositoryId);
        }
    }

    private ContributorEntity resolveContributor(RepositoryEntity repo, String login, String avatarUrl) {
        if (login != null) {
            return contributorRepository.findByGithubLoginAndRepositoryId(login, repo.getId())
                .orElseGet(() -> {
                    ContributorEntity c = new ContributorEntity();
                    c.setRepository(repo);
                    c.setGithubLogin(login);
                    c.setGithubAvatarUrl(avatarUrl);
                    c.setTotalCommits(0);
                    return contributorRepository.save(c);
                });
        }
        // anonymous commit — use a synthetic "unknown" contributor per repo
        return contributorRepository.findByGithubLoginAndRepositoryId("unknown", repo.getId())
            .orElseGet(() -> {
                ContributorEntity c = new ContributorEntity();
                c.setRepository(repo);
                c.setGithubLogin("unknown");
                c.setTotalCommits(0);
                return contributorRepository.save(c);
            });
    }
}
