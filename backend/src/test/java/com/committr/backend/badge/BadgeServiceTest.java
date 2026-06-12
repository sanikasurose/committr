package com.committr.backend.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.committr.backend.contributor.ContributorEntity;
import com.committr.backend.contributor.ContributorRepository;
import com.committr.backend.repository.RepositoryEntity;
import com.committr.backend.repository.RepositoryRepository;
import com.committr.backend.snapshot.SnapshotEntity;
import com.committr.backend.snapshot.SnapshotRepository;
import com.committr.backend.user.User;
import com.committr.backend.user.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock SnapshotRepository snapshotRepository;
    @Mock RepositoryRepository repositoryRepository;
    @Mock UserRepository userRepository;
    @Mock ContributorRepository contributorRepository;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks BadgeService badgeService;

    @BeforeEach
    void setUpRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void cacheHit_returnsCachedSvgWithoutQueryingDb() {
        when(valueOps.get("badge:alice")).thenReturn("<svg>cached</svg>");

        String result = badgeService.getBadgeSvg("alice");

        assertThat(result).isEqualTo("<svg>cached</svg>");
        verify(snapshotRepository, never()).findByRepositoryIdOrderByWeekStartAsc(anyLong());
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void cacheMiss_computesAggregatesAndCaches() {
        when(valueOps.get("badge:alice")).thenReturn(null);

        User user = new User();
        user.setGithubId(1L);
        user.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        RepositoryEntity repo = new RepositoryEntity();
        when(repositoryRepository.findByUser_IdAndDeletedAtIsNull(user.getId()))
            .thenReturn(List.of(repo));

        ContributorEntity contributor = new ContributorEntity();
        contributor.setGithubLogin("alice");
        contributor.setGithubAvatarUrl("https://avatars.example.com/alice");

        SnapshotEntity snapshot = new SnapshotEntity();
        snapshot.setContributor(contributor);
        snapshot.setWeekStart(LocalDate.of(2024, 1, 8));
        snapshot.setCommitCount(5);
        snapshot.setLinesAdded(200);
        snapshot.setLinesDeleted(50);
        snapshot.setLanguageDistribution(Map.of("Java", 5));
        when(snapshotRepository.findByRepositoryIdOrderByWeekStartAsc(repo.getId()))
            .thenReturn(List.of(snapshot));

        String result = badgeService.getBadgeSvg("alice");

        assertThat(result).contains("<svg");
        assertThat(result).contains("LINES SHIPPED");
        assertThat(result).contains("STREAK");
        assertThat(result).contains("Java");
        verify(valueOps).set(eq("badge:alice"), anyString(), eq(15L), eq(TimeUnit.MINUTES));
    }

    @Test
    void unknownUser_throws404() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> badgeService.getBadgeSvg("nobody"))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void evictBadgeCacheForRepo_deletesEachContributorKey() {
        ContributorEntity c1 = new ContributorEntity();
        c1.setGithubLogin("alice");
        ContributorEntity c2 = new ContributorEntity();
        c2.setGithubLogin("bob");
        when(contributorRepository.findByRepositoryId(42L)).thenReturn(List.of(c1, c2));

        badgeService.evictBadgeCacheForRepo(42L);

        verify(redisTemplate).delete("badge:alice");
        verify(redisTemplate).delete("badge:bob");
        verify(redisTemplate, times(2)).delete(anyString());
    }
}
