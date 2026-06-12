package com.committr.backend.snapshot;

import com.committr.backend.contributor.ContributorEntity;
import com.committr.backend.repository.RepositoryEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "snapshots")
public class SnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id", nullable = false)
    private RepositoryEntity repository;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "contributor_id", nullable = false)
    private ContributorEntity contributor;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "commit_count", nullable = false)
    private int commitCount = 0;

    @Column(name = "lines_added", nullable = false)
    private int linesAdded = 0;

    @Column(name = "lines_deleted", nullable = false)
    private int linesDeleted = 0;

    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(name = "pr_merge_time", columnDefinition = "interval")
    private Duration prMergeTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "language_distribution", columnDefinition = "jsonb")
    private Map<String, Integer> languageDistribution;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "coding_hours", columnDefinition = "jsonb")
    private List<Integer> codingHours;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RepositoryEntity getRepository() {
        return repository;
    }

    public void setRepository(RepositoryEntity repository) {
        this.repository = repository;
    }

    public ContributorEntity getContributor() {
        return contributor;
    }

    public void setContributor(ContributorEntity contributor) {
        this.contributor = contributor;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }

    public int getCommitCount() {
        return commitCount;
    }

    public void setCommitCount(int commitCount) {
        this.commitCount = commitCount;
    }

    public int getLinesAdded() {
        return linesAdded;
    }

    public void setLinesAdded(int linesAdded) {
        this.linesAdded = linesAdded;
    }

    public int getLinesDeleted() {
        return linesDeleted;
    }

    public void setLinesDeleted(int linesDeleted) {
        this.linesDeleted = linesDeleted;
    }

    public Duration getPrMergeTime() {
        return prMergeTime;
    }

    public void setPrMergeTime(Duration prMergeTime) {
        this.prMergeTime = prMergeTime;
    }

    public Map<String, Integer> getLanguageDistribution() {
        return languageDistribution;
    }

    public void setLanguageDistribution(Map<String, Integer> languageDistribution) {
        this.languageDistribution = languageDistribution;
    }

    public List<Integer> getCodingHours() {
        return codingHours;
    }

    public void setCodingHours(List<Integer> codingHours) {
        this.codingHours = codingHours;
    }
}
