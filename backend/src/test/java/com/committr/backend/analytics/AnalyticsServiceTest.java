package com.committr.backend.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.committr.backend.contributor.ContributorEntity;
import com.committr.backend.dto.analytics.CodingHoursDto;
import com.committr.backend.dto.analytics.CommitFrequencyDto;
import com.committr.backend.dto.analytics.ContributorShareDto;
import com.committr.backend.dto.analytics.PrVelocityDto;
import com.committr.backend.repository.RepositoryEntity;
import com.committr.backend.session.SessionAuthenticationToken;
import com.committr.backend.session.SessionUserDto;
import com.committr.backend.snapshot.SnapshotEntity;
import com.committr.backend.snapshot.SnapshotRepository;
import com.committr.backend.user.User;
import com.committr.backend.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.committr.backend.repository.RepositoryRepository;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock SnapshotRepository snapshotRepository;
    @Mock RepositoryRepository repositoryRepository;
    @Mock UserRepository userRepository;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock ObjectMapper objectMapper;

    @InjectMocks AnalyticsService analyticsService;

    private User testUser;
    private RepositoryEntity testRepo;

    @BeforeEach
    void setUpSecurityContext() {
        testUser = new User();
        testUser.setGithubId(1L);
        testUser.setUsername("testuser");

        testRepo = new RepositoryEntity();
        // deletedAt is null by default (not set)

        SessionUserDto sessionUser = new SessionUserDto(1L, "testuser", null);
        SessionAuthenticationToken auth = new SessionAuthenticationToken(sessionUser);
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(repositoryRepository.findByIdAndUser_Id(anyLong(), anyLong()))
            .thenReturn(Optional.of(testRepo));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- getContributors ---

    @Test
    void getContributors_cacheMiss_sharePercentsCorrect() throws Exception {
        when(valueOps.get(anyString())).thenReturn(null);

        ContributorEntity c1 = contributor("alice");
        ContributorEntity c2 = contributor("bob");

        List<SnapshotEntity> snapshots = List.of(
            snapshot(c1, LocalDate.of(2024, 1, 8), 70, 1000, 200, null, null),
            snapshot(c2, LocalDate.of(2024, 1, 8), 30, 500, 100, null, null)
        );
        when(snapshotRepository.findByRepositoryIdOrderByWeekStartAsc(anyLong())).thenReturn(snapshots);

        List<ContributorShareDto> result = analyticsService.getContributors(1L);

        assertThat(result).hasSize(2);
        double totalShare = result.stream().mapToDouble(ContributorShareDto::sharePercent).sum();
        assertThat(totalShare).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.1));
    }

    @Test
    void getContributors_emptySnapshots_returnsEmptyList() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(snapshotRepository.findByRepositoryIdOrderByWeekStartAsc(anyLong())).thenReturn(List.of());

        List<ContributorShareDto> result = analyticsService.getContributors(1L);

        assertThat(result).isEmpty();
    }

    // --- getCodingHours ---

    @Test
    void getCodingHours_sumsBucketsAcrossSnapshots() {
        when(valueOps.get(anyString())).thenReturn(null);

        List<Integer> hours1 = new ArrayList<>(java.util.Collections.nCopies(24, 0));
        hours1.set(10, 3);
        List<Integer> hours2 = new ArrayList<>(java.util.Collections.nCopies(24, 0));
        hours2.set(10, 2);
        hours2.set(14, 1);

        ContributorEntity c = contributor("alice");
        List<SnapshotEntity> snapshots = List.of(
            snapshot(c, LocalDate.of(2024, 1, 8), 3, 100, 0, null, hours1),
            snapshot(c, LocalDate.of(2024, 1, 15), 3, 200, 0, null, hours2)
        );
        when(snapshotRepository.findByRepositoryIdOrderByWeekStartAsc(anyLong())).thenReturn(snapshots);

        CodingHoursDto result = analyticsService.getCodingHours(1L);

        assertThat(result.hours()).hasSize(24);
        assertThat(result.hours().get(10)).isEqualTo(5);
        assertThat(result.hours().get(14)).isEqualTo(1);
    }

    // --- getCommitFrequency ---

    @Test
    void getCommitFrequency_twoSnapshotsSameWeek_groupedIntoOne() {
        when(valueOps.get(anyString())).thenReturn(null);

        ContributorEntity c1 = contributor("alice");
        ContributorEntity c2 = contributor("bob");
        LocalDate week = LocalDate.of(2024, 1, 8);
        List<SnapshotEntity> snapshots = List.of(
            snapshot(c1, week, 5, 100, 20, null, null),
            snapshot(c2, week, 3, 50, 10, null, null)
        );
        when(snapshotRepository.findByRepositoryIdOrderByWeekStartAsc(anyLong())).thenReturn(snapshots);

        List<CommitFrequencyDto> result = analyticsService.getCommitFrequency(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).commitCount()).isEqualTo(8);
        assertThat(result.get(0).linesAdded()).isEqualTo(150);
        assertThat(result.get(0).linesDeleted()).isEqualTo(30);
    }

    // --- getPrVelocity ---

    @Test
    void getPrVelocity_excludesSnapshotsWithNullMergeTime() {
        when(valueOps.get(anyString())).thenReturn(null);

        ContributorEntity c = contributor("alice");
        List<SnapshotEntity> snapshots = List.of(
            snapshot(c, LocalDate.of(2024, 1, 8), 5, 100, 0, Duration.ofHours(2), null),
            snapshot(c, LocalDate.of(2024, 1, 15), 3, 50, 0, null, null)  // null prMergeTime excluded
        );
        when(snapshotRepository.findByRepositoryIdOrderByWeekStartAsc(anyLong())).thenReturn(snapshots);

        List<PrVelocityDto> result = analyticsService.getPrVelocity(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).avgMergeHours()).isEqualTo(2.0);
    }

    // --- helpers ---

    private static ContributorEntity contributor(String login) {
        ContributorEntity c = new ContributorEntity();
        c.setGithubLogin(login);
        c.setGithubAvatarUrl("https://avatars.example.com/" + login);
        return c;
    }

    private static SnapshotEntity snapshot(
        ContributorEntity contributor,
        LocalDate weekStart,
        int commitCount,
        int linesAdded,
        int linesDeleted,
        Duration prMergeTime,
        List<Integer> codingHours
    ) {
        SnapshotEntity s = new SnapshotEntity();
        s.setContributor(contributor);
        s.setWeekStart(weekStart);
        s.setCommitCount(commitCount);
        s.setLinesAdded(linesAdded);
        s.setLinesDeleted(linesDeleted);
        s.setPrMergeTime(prMergeTime);
        s.setCodingHours(codingHours);
        s.setLanguageDistribution(Map.of("Java", commitCount));
        return s;
    }

}
