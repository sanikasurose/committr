package com.committr.backend.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.committr.backend.commit.CommitEntity;
import com.committr.backend.commit.CommitRepository;
import com.committr.backend.contributor.ContributorRepository;
import com.committr.backend.crypto.EncryptionService;
import com.committr.backend.dto.github.CommitSummary;
import com.committr.backend.dto.github.ContributorSummary;
import com.committr.backend.dto.github.PullRequestSummary;
import com.committr.backend.github.GitHubClient;
import com.committr.backend.prEvent.PrEventRepository;
import com.committr.backend.repository.RepositoryEntity;
import com.committr.backend.repository.RepositoryRepository;
import com.committr.backend.snapshot.SnapshotEntity;
import com.committr.backend.snapshot.SnapshotRepository;
import com.committr.backend.user.User;
import com.committr.backend.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("integration")
@Testcontainers
class IngestionServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    GitHubClient gitHubClient;

    @MockitoBean
    StringRedisTemplate redisTemplate;

    @Autowired
    IngestionService ingestionService;

    @Autowired
    EncryptionService encryptionService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    CommitRepository commitRepository;

    @Autowired
    SnapshotRepository snapshotRepository;

    @Autowired
    ContributorRepository contributorRepository;

    @Autowired
    PrEventRepository prEventRepository;

    private User savedUser;
    private RepositoryEntity savedRepo;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());

        User user = new User();
        user.setGithubId(99999L);
        user.setUsername("testowner");
        user.setAvatarUrl("https://example.com/avatar");
        user.setEncryptedAccessToken(encryptionService.encrypt("fake-token"));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        savedUser = userRepository.save(user);

        RepositoryEntity repo = new RepositoryEntity();
        repo.setUser(savedUser);
        repo.setGithubRepoId(12345L);
        repo.setName("testrepo");
        repo.setFullName("testowner/testrepo");
        repo.setOwnerLogin("testowner");
        repo.setPrivate(false);
        repo.setHtmlUrl("https://github.com/testowner/testrepo");
        repo.setCreatedAt(LocalDateTime.now());
        repo.setUpdatedAt(LocalDateTime.now());
        savedRepo = repositoryRepository.save(repo);

        when(gitHubClient.getContributors("testowner", "testrepo", "fake-token"))
            .thenReturn(List.of(new ContributorSummary("alice", "https://avatars.example.com/alice", 3)));

        when(gitHubClient.getCommits("testowner", "testrepo", "fake-token"))
            .thenReturn(List.of(
                new CommitSummary(
                    "sha-alice-1", "alice", "https://avatars.example.com/alice",
                    Instant.parse("2024-01-08T10:00:00Z"), "feat: first", 100, 20, "Java"),
                new CommitSummary(
                    "sha-alice-2", "alice", "https://avatars.example.com/alice",
                    Instant.parse("2024-01-15T11:00:00Z"), "fix: second", 50, 10, "Java")
            ));

        when(gitHubClient.getMergedPullRequests("testowner", "testrepo", "fake-token"))
            .thenReturn(List.of(new PullRequestSummary(
                1,
                Instant.parse("2024-01-08T09:00:00Z"),
                Instant.parse("2024-01-08T11:00:00Z")
            )));
    }

    @AfterEach
    void tearDown() {
        snapshotRepository.deleteAll();
        prEventRepository.deleteAll();
        commitRepository.deleteAll();
        contributorRepository.deleteAll();
        repositoryRepository.deleteById(savedRepo.getId());
        userRepository.deleteById(savedUser.getId());
    }

    @Test
    void ingest_persistsContributor() {
        ingestionService.ingest(savedRepo.getId());

        assertThat(contributorRepository.findByGithubLoginAndRepositoryId("alice", savedRepo.getId()))
            .isPresent();
    }

    @Test
    void ingest_persistsCommits_withCorrectFields() {
        ingestionService.ingest(savedRepo.getId());

        List<CommitEntity> commits =
            commitRepository.findByRepositoryIdOrderByCommittedAtDesc(savedRepo.getId());
        assertThat(commits).hasSize(2);

        CommitEntity first = commits.stream()
            .filter(c -> "sha-alice-1".equals(c.getGithubCommitId()))
            .findFirst()
            .orElseThrow();
        assertThat(first.getLinesAdded()).isEqualTo(100);
        assertThat(first.getLinesDeleted()).isEqualTo(20);
        assertThat(first.getLanguage()).isEqualTo("Java");
    }

    @Test
    void ingest_createsSnapshots_onePerContributorPerWeek() {
        ingestionService.ingest(savedRepo.getId());

        List<SnapshotEntity> snapshots =
            snapshotRepository.findByRepositoryIdOrderByWeekStartAsc(savedRepo.getId());
        assertThat(snapshots).hasSize(2);

        SnapshotEntity firstWeek = snapshots.get(0);
        assertThat(firstWeek.getWeekStart()).isEqualTo(LocalDate.of(2024, 1, 8));
        assertThat(firstWeek.getCommitCount()).isEqualTo(1);
        assertThat(firstWeek.getLinesAdded()).isEqualTo(100);
    }

    @Test
    void ingest_prMergeTime_nonNull() {
        ingestionService.ingest(savedRepo.getId());

        List<SnapshotEntity> snapshots =
            snapshotRepository.findByRepositoryIdOrderByWeekStartAsc(savedRepo.getId());
        SnapshotEntity firstWeek = snapshots.stream()
            .filter(s -> s.getWeekStart().equals(LocalDate.of(2024, 1, 8)))
            .findFirst()
            .orElseThrow();

        assertThat(firstWeek.getPrMergeTime()).isNotNull();
        assertThat(firstWeek.getPrMergeTime().toHours()).isEqualTo(2L);
    }

    @Test
    void ingest_isIdempotent_noDuplicateCommits() {
        ingestionService.ingest(savedRepo.getId());
        ingestionService.ingest(savedRepo.getId());

        List<CommitEntity> commits =
            commitRepository.findByRepositoryIdOrderByCommittedAtDesc(savedRepo.getId());
        assertThat(commits).hasSize(2);
    }
}
