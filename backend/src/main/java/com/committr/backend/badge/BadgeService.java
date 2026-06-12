package com.committr.backend.badge;

import com.committr.backend.contributor.ContributorEntity;
import com.committr.backend.contributor.ContributorRepository;
import com.committr.backend.repository.RepositoryEntity;
import com.committr.backend.repository.RepositoryRepository;
import com.committr.backend.snapshot.SnapshotEntity;
import com.committr.backend.snapshot.SnapshotRepository;
import com.committr.backend.user.User;
import com.committr.backend.user.UserRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BadgeService {

    private static final Duration BADGE_TTL = Duration.ofMinutes(15);
    private static final String BADGE_KEY_PREFIX = "badge:";

    private final SnapshotRepository snapshotRepository;
    private final RepositoryRepository repositoryRepository;
    private final UserRepository userRepository;
    private final ContributorRepository contributorRepository;
    private final StringRedisTemplate redisTemplate;

    public BadgeService(
        SnapshotRepository snapshotRepository,
        RepositoryRepository repositoryRepository,
        UserRepository userRepository,
        ContributorRepository contributorRepository,
        StringRedisTemplate redisTemplate
    ) {
        this.snapshotRepository = snapshotRepository;
        this.repositoryRepository = repositoryRepository;
        this.userRepository = userRepository;
        this.contributorRepository = contributorRepository;
        this.redisTemplate = redisTemplate;
    }

    public String getBadgeSvg(String username) {
        String key = BADGE_KEY_PREFIX + username;

        // Check Redis cache first
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        // Cache miss: compute and store
        BadgeData data = computeBadgeData(username);
        String svg = renderSvg(data);
        redisTemplate.opsForValue().set(key, svg, BADGE_TTL.toMinutes(), TimeUnit.MINUTES);
        return svg;
    }

    public void evictBadgeCacheForRepo(Long repositoryId) {
        List<ContributorEntity> contributors = contributorRepository.findByRepositoryId(repositoryId);
        for (ContributorEntity c : contributors) {
            redisTemplate.delete(BADGE_KEY_PREFIX + c.getGithubLogin());
        }
    }

    private BadgeData computeBadgeData(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<RepositoryEntity> repos = repositoryRepository.findByUser_IdAndDeletedAtIsNull(user.getId());

        long totalLinesShipped = 0;
        long totalUserCommits = 0;
        long totalAllCommits = 0;
        List<LocalDate> allWeekStarts = new ArrayList<>();
        Map<String, Integer> mergedLanguageDist = new HashMap<>();

        for (RepositoryEntity repo : repos) {
            List<SnapshotEntity> allSnapshots = snapshotRepository
                .findByRepositoryIdOrderByWeekStartAsc(repo.getId());

            for (SnapshotEntity snapshot : allSnapshots) {
                totalAllCommits += snapshot.getCommitCount();

                if (snapshot.getContributor().getGithubLogin().equals(username)) {
                    totalLinesShipped += snapshot.getLinesAdded() + snapshot.getLinesDeleted();
                    totalUserCommits += snapshot.getCommitCount();
                    allWeekStarts.add(snapshot.getWeekStart());

                    Map<String, Integer> langDist = snapshot.getLanguageDistribution();
                    if (langDist != null) {
                        for (Map.Entry<String, Integer> entry : langDist.entrySet()) {
                            mergedLanguageDist.merge(entry.getKey(), entry.getValue(), Integer::sum);
                        }
                    }
                }
            }
        }

        int longestStreak = StreakCalculator.longestConsecutiveWeeks(allWeekStarts);

        String topLanguage = "N/A";
        if (!mergedLanguageDist.isEmpty()) {
            topLanguage = mergedLanguageDist.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        }

        double teamSharePercent = 0.0;
        if (totalAllCommits > 0) {
            teamSharePercent = (double) totalUserCommits / totalAllCommits * 100.0;
        }
        teamSharePercent = Math.round(teamSharePercent * 10.0) / 10.0;

        return new BadgeData(username, totalLinesShipped, longestStreak, topLanguage, teamSharePercent);
    }

    private String renderSvg(BadgeData data) {
        String linesFormatted = String.format("%,d", data.totalLinesShipped());
        String streakFormatted = data.longestStreak() + " weeks";
        String langTruncated = data.topLanguage().length() > 10
            ? data.topLanguage().substring(0, 10)
            : data.topLanguage();
        String teamShareFormatted = String.format("%.1f%%", data.teamSharePercent());

        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"520\" height=\"60\">");
        sb.append("<rect width=\"520\" height=\"60\" rx=\"6\" fill=\"#1e1e2e\"/>");

        // Separator lines
        sb.append("<line x1=\"130\" y1=\"8\" x2=\"130\" y2=\"52\" stroke=\"#333\" stroke-width=\"1\"/>");
        sb.append("<line x1=\"260\" y1=\"8\" x2=\"260\" y2=\"52\" stroke=\"#333\" stroke-width=\"1\"/>");
        sb.append("<line x1=\"390\" y1=\"8\" x2=\"390\" y2=\"52\" stroke=\"#333\" stroke-width=\"1\"/>");

        // Tile 1: LINES SHIPPED (center x=65)
        sb.append("<text x=\"65\" y=\"22\" font-family=\"monospace\" font-size=\"10\" fill=\"#aaa\" text-anchor=\"middle\">LINES SHIPPED</text>");
        sb.append("<text x=\"65\" y=\"46\" font-family=\"monospace\" font-size=\"18\" fill=\"#fff\" text-anchor=\"middle\">")
          .append(escapeXml(linesFormatted))
          .append("</text>");

        // Tile 2: STREAK (center x=195)
        sb.append("<text x=\"195\" y=\"22\" font-family=\"monospace\" font-size=\"10\" fill=\"#aaa\" text-anchor=\"middle\">STREAK</text>");
        sb.append("<text x=\"195\" y=\"46\" font-family=\"monospace\" font-size=\"18\" fill=\"#fff\" text-anchor=\"middle\">")
          .append(escapeXml(streakFormatted))
          .append("</text>");

        // Tile 3: TOP LANG (center x=325)
        sb.append("<text x=\"325\" y=\"22\" font-family=\"monospace\" font-size=\"10\" fill=\"#aaa\" text-anchor=\"middle\">TOP LANG</text>");
        sb.append("<text x=\"325\" y=\"46\" font-family=\"monospace\" font-size=\"18\" fill=\"#fff\" text-anchor=\"middle\">")
          .append(escapeXml(langTruncated))
          .append("</text>");

        // Tile 4: TEAM SHARE (center x=455)
        sb.append("<text x=\"455\" y=\"22\" font-family=\"monospace\" font-size=\"10\" fill=\"#aaa\" text-anchor=\"middle\">TEAM SHARE</text>");
        sb.append("<text x=\"455\" y=\"46\" font-family=\"monospace\" font-size=\"18\" fill=\"#fff\" text-anchor=\"middle\">")
          .append(escapeXml(teamShareFormatted))
          .append("</text>");

        sb.append("</svg>");
        return sb.toString();
    }

    private static String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
