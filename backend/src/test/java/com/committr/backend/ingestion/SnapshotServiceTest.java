package com.committr.backend.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.committr.backend.badge.BadgeService;
import com.committr.backend.commit.CommitEntity;
import com.committr.backend.commit.CommitRepository;
import com.committr.backend.contributor.ContributorEntity;
import com.committr.backend.prEvent.PrEventRepository;
import com.committr.backend.repository.RepositoryEntity;
import com.committr.backend.repository.RepositoryRepository;
import com.committr.backend.snapshot.SnapshotEntity;
import com.committr.backend.snapshot.SnapshotRepository;
import com.committr.backend.user.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SnapshotServiceTest {

    @Mock RepositoryRepository repositoryRepository;
    @Mock CommitRepository commitRepository;
    @Mock PrEventRepository prEventRepository;
    @Mock SnapshotRepository snapshotRepository;
    @Mock BadgeService badgeService;

    @InjectMocks SnapshotService snapshotService;

    @Test
    void buildSnapshots_repoNotFound_throwsIllegalArgument() {
        when(repositoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> snapshotService.buildSnapshots(99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Repository not found");
    }

    @Test
    void buildSnapshots_noCommits_doesNotSaveAnySnapshot() {
        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(repo()));
        when(commitRepository.findByRepositoryIdOrderByCommittedAtDesc(1L)).thenReturn(List.of());
        when(prEventRepository.findByRepositoryIdAndMergedAtIsNotNullOrderByMergedAtAsc(1L))
            .thenReturn(List.of());

        snapshotService.buildSnapshots(1L);

        verify(snapshotRepository, never()).save(any());
    }

    @Test
    void buildSnapshots_singleCommit_createsSnapshotWithCorrectFields() {
        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(repo()));

        ContributorEntity alice = contributor(10L, "alice");
        CommitEntity commit = commit(alice, "2024-01-08T10:00:00Z", 100, 20, "Java");
        when(commitRepository.findByRepositoryIdOrderByCommittedAtDesc(1L)).thenReturn(List.of(commit));
        when(prEventRepository.findByRepositoryIdAndMergedAtIsNotNullOrderByMergedAtAsc(1L))
            .thenReturn(List.of());
        when(snapshotRepository.findByRepositoryIdAndContributorIdAndWeekStart(anyLong(), anyLong(), any()))
            .thenReturn(Optional.empty());

        snapshotService.buildSnapshots(1L);

        ArgumentCaptor<SnapshotEntity> captor = ArgumentCaptor.forClass(SnapshotEntity.class);
        verify(snapshotRepository).save(captor.capture());
        SnapshotEntity saved = captor.getValue();
        assertThat(saved.getCommitCount()).isEqualTo(1);
        assertThat(saved.getLinesAdded()).isEqualTo(100);
        assertThat(saved.getLinesDeleted()).isEqualTo(20);
        assertThat(saved.getWeekStart()).isEqualTo(LocalDate.of(2024, 1, 8));
    }

    @Test
    void buildSnapshots_twoContributorsSameWeek_createsTwoSnapshots() {
        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(repo()));

        ContributorEntity alice = contributor(10L, "alice");
        ContributorEntity bob = contributor(11L, "bob");
        CommitEntity c1 = commit(alice, "2024-01-08T10:00:00Z", 50, 0, "Java");
        CommitEntity c2 = commit(bob, "2024-01-09T11:00:00Z", 30, 5, "Python");
        when(commitRepository.findByRepositoryIdOrderByCommittedAtDesc(1L)).thenReturn(List.of(c1, c2));
        when(prEventRepository.findByRepositoryIdAndMergedAtIsNotNullOrderByMergedAtAsc(1L))
            .thenReturn(List.of());
        when(snapshotRepository.findByRepositoryIdAndContributorIdAndWeekStart(anyLong(), anyLong(), any()))
            .thenReturn(Optional.empty());

        snapshotService.buildSnapshots(1L);

        verify(snapshotRepository, org.mockito.Mockito.times(2)).save(any(SnapshotEntity.class));
    }

    @Test
    void buildSnapshots_evictsBadgeCacheAfterSave() {
        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(repo()));
        when(commitRepository.findByRepositoryIdOrderByCommittedAtDesc(1L)).thenReturn(List.of());
        when(prEventRepository.findByRepositoryIdAndMergedAtIsNotNullOrderByMergedAtAsc(1L))
            .thenReturn(List.of());

        snapshotService.buildSnapshots(1L);

        verify(badgeService).evictBadgeCacheForRepo(1L);
    }

    // --- helpers ---

    private static RepositoryEntity repo() {
        User user = new User();
        user.setGithubId(99L);
        user.setUsername("owner");
        user.setEncryptedAccessToken("enc-token");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        RepositoryEntity r = new RepositoryEntity();
        r.setUser(user);
        r.setGithubRepoId(1L);
        r.setName("testrepo");
        r.setFullName("owner/testrepo");
        r.setOwnerLogin("owner");
        r.setPrivate(false);
        r.setHtmlUrl("https://github.com/owner/testrepo");
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    private static ContributorEntity contributor(Long id, String login) {
        ContributorEntity c = new ContributorEntity();
        c.setId(id);
        c.setGithubLogin(login);
        c.setGithubAvatarUrl("https://avatars.example.com/" + login);
        return c;
    }

    private static CommitEntity commit(ContributorEntity contributor,
                                       String instantStr, int added, int deleted, String lang) {
        CommitEntity c = new CommitEntity();
        c.setContributor(contributor);
        c.setGithubCommitId("sha-" + Math.random());
        c.setMessage("test commit");
        c.setLinesAdded(added);
        c.setLinesDeleted(deleted);
        c.setLanguage(lang);
        c.setCommittedAt(java.time.Instant.parse(instantStr).atZone(ZoneOffset.UTC).toLocalDateTime());
        return c;
    }
}
