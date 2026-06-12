package com.committr.backend.analytics;

import com.committr.backend.contributor.ContributorEntity;
import com.committr.backend.dto.analytics.CodingHoursDto;
import com.committr.backend.dto.analytics.CommitFrequencyDto;
import com.committr.backend.dto.analytics.ContributorShareDto;
import com.committr.backend.dto.analytics.LanguageTrendDto;
import com.committr.backend.dto.analytics.PrVelocityDto;
import com.committr.backend.repository.RepositoryEntity;
import com.committr.backend.repository.RepositoryRepository;
import com.committr.backend.session.SessionAuthenticationToken;
import com.committr.backend.session.SessionUserDto;
import com.committr.backend.snapshot.SnapshotEntity;
import com.committr.backend.snapshot.SnapshotRepository;
import com.committr.backend.user.User;
import com.committr.backend.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AnalyticsService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final SnapshotRepository snapshotRepository;
    private final RepositoryRepository repositoryRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AnalyticsService(
        SnapshotRepository snapshotRepository,
        RepositoryRepository repositoryRepository,
        UserRepository userRepository,
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper
    ) {
        this.snapshotRepository = snapshotRepository;
        this.repositoryRepository = repositoryRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ContributorShareDto> getContributors(Long repositoryId) {
        requireOwnedRepository(repositoryId);
        String key = "contributors:" + repositoryId;
        List<ContributorShareDto> cached = readListCache(key, new TypeReference<>() {});
        if (cached != null) return cached;

        List<SnapshotEntity> snapshots = snapshotRepository.findByRepositoryIdOrderByWeekStartAsc(repositoryId);

        Map<Long, ContributorAccumulator> byContributor = new LinkedHashMap<>();
        int totalCommits = 0;
        for (SnapshotEntity s : snapshots) {
            ContributorEntity c = s.getContributor();
            ContributorAccumulator acc = byContributor.computeIfAbsent(c.getId(), id -> new ContributorAccumulator(c));
            acc.commitCount += s.getCommitCount();
            acc.linesAdded += s.getLinesAdded();
            acc.linesDeleted += s.getLinesDeleted();
            totalCommits += s.getCommitCount();
        }

        List<ContributorShareDto> result = new ArrayList<>(byContributor.size());
        for (ContributorAccumulator acc : byContributor.values()) {
            double share = totalCommits == 0 ? 0.0 : (acc.commitCount * 100.0 / totalCommits);
            result.add(new ContributorShareDto(
                acc.contributor.getGithubLogin(),
                acc.contributor.getGithubAvatarUrl(),
                acc.commitCount,
                acc.linesAdded,
                acc.linesDeleted,
                Math.round(share * 100.0) / 100.0
            ));
        }
        writeCache(key, result);
        return result;
    }

    @Transactional(readOnly = true)
    public List<LanguageTrendDto> getLanguageTrend(Long repositoryId) {
        requireOwnedRepository(repositoryId);
        String key = "langtrend:" + repositoryId;
        List<LanguageTrendDto> cached = readListCache(key, new TypeReference<>() {});
        if (cached != null) return cached;

        List<SnapshotEntity> snapshots = snapshotRepository.findByRepositoryIdOrderByWeekStartAsc(repositoryId);

        Map<LocalDate, Map<String, Integer>> byWeek = new TreeMap<>();
        for (SnapshotEntity s : snapshots) {
            Map<String, Integer> dist = s.getLanguageDistribution();
            if (dist == null || dist.isEmpty()) continue;
            Map<String, Integer> weekMap = byWeek.computeIfAbsent(s.getWeekStart(), w -> new HashMap<>());
            for (Map.Entry<String, Integer> e : dist.entrySet()) {
                weekMap.merge(e.getKey(), e.getValue(), Integer::sum);
            }
        }

        List<LanguageTrendDto> result = new ArrayList<>(byWeek.size());
        for (Map.Entry<LocalDate, Map<String, Integer>> e : byWeek.entrySet()) {
            result.add(new LanguageTrendDto(e.getKey(), e.getValue()));
        }
        writeCache(key, result);
        return result;
    }

    @Transactional(readOnly = true)
    public CodingHoursDto getCodingHours(Long repositoryId) {
        requireOwnedRepository(repositoryId);
        String key = "timeheat:" + repositoryId;
        CodingHoursDto cached = readCache(key, CodingHoursDto.class);
        if (cached != null) return cached;

        List<SnapshotEntity> snapshots = snapshotRepository.findByRepositoryIdOrderByWeekStartAsc(repositoryId);

        List<Integer> hours = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) hours.add(0);
        for (SnapshotEntity s : snapshots) {
            List<Integer> ch = s.getCodingHours();
            if (ch == null) continue;
            int len = Math.min(ch.size(), 24);
            for (int i = 0; i < len; i++) {
                hours.set(i, hours.get(i) + ch.get(i));
            }
        }

        CodingHoursDto result = new CodingHoursDto(hours);
        writeCache(key, result);
        return result;
    }

    @Transactional(readOnly = true)
    public List<CommitFrequencyDto> getCommitFrequency(Long repositoryId) {
        requireOwnedRepository(repositoryId);
        String key = "freq:" + repositoryId;
        List<CommitFrequencyDto> cached = readListCache(key, new TypeReference<>() {});
        if (cached != null) return cached;

        List<SnapshotEntity> snapshots = snapshotRepository.findByRepositoryIdOrderByWeekStartAsc(repositoryId);

        Map<LocalDate, int[]> byWeek = new TreeMap<>();
        for (SnapshotEntity s : snapshots) {
            int[] agg = byWeek.computeIfAbsent(s.getWeekStart(), w -> new int[3]);
            agg[0] += s.getCommitCount();
            agg[1] += s.getLinesAdded();
            agg[2] += s.getLinesDeleted();
        }

        List<CommitFrequencyDto> result = new ArrayList<>(byWeek.size());
        for (Map.Entry<LocalDate, int[]> e : byWeek.entrySet()) {
            int[] v = e.getValue();
            result.add(new CommitFrequencyDto(e.getKey(), v[0], v[1], v[2]));
        }
        writeCache(key, result);
        return result;
    }

    @Transactional(readOnly = true)
    public List<PrVelocityDto> getPrVelocity(Long repositoryId) {
        requireOwnedRepository(repositoryId);
        String key = "prvelocity:" + repositoryId;
        List<PrVelocityDto> cached = readListCache(key, new TypeReference<>() {});
        if (cached != null) return cached;

        List<SnapshotEntity> snapshots = snapshotRepository.findByRepositoryIdOrderByWeekStartAsc(repositoryId);

        Map<LocalDate, double[]> byWeek = new TreeMap<>();
        for (SnapshotEntity s : snapshots) {
            Duration mt = s.getPrMergeTime();
            if (mt == null) continue;
            double[] agg = byWeek.computeIfAbsent(s.getWeekStart(), w -> new double[2]);
            agg[0] += mt.toMinutes() / 60.0;
            agg[1] += 1;
        }

        List<PrVelocityDto> result = new ArrayList<>(byWeek.size());
        for (Map.Entry<LocalDate, double[]> e : byWeek.entrySet()) {
            double[] v = e.getValue();
            double avg = v[1] == 0 ? 0.0 : v[0] / v[1];
            result.add(new PrVelocityDto(e.getKey(), Math.round(avg * 100.0) / 100.0));
        }
        writeCache(key, result);
        return result;
    }

    private RepositoryEntity requireOwnedRepository(Long repositoryId) {
        User user = requireCurrentUser();
        return repositoryRepository.findByIdAndUser_Id(repositoryId, user.getId())
            .filter(r -> r.getDeletedAt() == null)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));
    }

    private User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth instanceof SessionAuthenticationToken sessionToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        SessionUserDto sessionUser = (SessionUserDto) sessionToken.getPrincipal();
        return userRepository.findById(sessionUser.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private <T> T readCache(String key, Class<T> type) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private <T> List<T> readListCache(String key, TypeReference<List<T>> typeRef) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private void writeCache(String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
        } catch (JsonProcessingException ex) {
            // skip caching on serialization failure
        }
    }

    private static class ContributorAccumulator {
        final ContributorEntity contributor;
        int commitCount;
        int linesAdded;
        int linesDeleted;

        ContributorAccumulator(ContributorEntity contributor) {
            this.contributor = contributor;
        }
    }
}
