package com.committr.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RepositoryService {

    private final RepositoryRepository repositoryRepository;

    public RepositoryService(RepositoryRepository repositoryRepository) {
        this.repositoryRepository = repositoryRepository;
    }

    @Transactional(readOnly = true)
    public List<RepositoryEntity> getUserRepositories(Long userId) {
        return repositoryRepository.findByUserIdAndDeletedAtIsNull(userId);
    }

    @Transactional
    public RepositoryEntity addRepository(String fullName, Long userId) {
        String trimmed = fullName == null ? "" : fullName.trim();
        ParsedFullName parsed = parseFullName(trimmed);

        long githubRepoId = mockGithubRepoId(parsed);
        if (repositoryRepository.existsByUserIdAndGithubRepoId(userId, githubRepoId)) {
            RepositoryEntity existing = repositoryRepository
                .findByUserIdAndGithubRepoId(userId, githubRepoId)
                .orElseThrow(() -> new IllegalStateException("Repository row missing after exists check"));
            if (existing.getDeletedAt() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Repository already added");
            }
            applyMockGithubFields(existing, parsed, githubRepoId);
            existing.setDeletedAt(null);
            return repositoryRepository.save(existing);
        }

        RepositoryEntity entity = new RepositoryEntity();
        entity.setUserId(userId);
        applyMockGithubFields(entity, parsed, githubRepoId);
        return repositoryRepository.save(entity);
    }

    @Transactional
    public void deleteRepository(Long id, Long userId) {
        RepositoryEntity entity = repositoryRepository
            .findByIdAndUserId(id, userId)
            .filter(r -> r.getDeletedAt() == null)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));
        entity.setDeletedAt(LocalDateTime.now());
        repositoryRepository.save(entity);
    }

    private static void applyMockGithubFields(RepositoryEntity target, ParsedFullName parsed, long githubRepoId) {
        target.setGithubRepoId(githubRepoId);
        target.setName(parsed.repo());
        target.setFullName(parsed.owner() + "/" + parsed.repo());
        target.setOwnerLogin(parsed.owner());
        target.setPrivate(false);
        target.setHtmlUrl("https://github.com/" + parsed.owner() + "/" + parsed.repo());
    }

    /**
     * Deterministic mock GitHub repository id until the real API is wired in (phase 2.4).
     */
    private static long mockGithubRepoId(ParsedFullName parsed) {
        return Integer.toUnsignedLong(Objects.hash(parsed.owner(), parsed.repo()));
    }

    private static ParsedFullName parseFullName(String trimmed) {
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fullName is required");
        }
        int slash = trimmed.indexOf('/');
        if (slash <= 0 || slash == trimmed.length() - 1) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "fullName must be in the form owner/repo"
            );
        }
        String owner = trimmed.substring(0, slash).strip();
        String repo = trimmed.substring(slash + 1).strip();
        if (owner.isBlank() || repo.isBlank() || repo.indexOf('/') >= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "fullName must be in the form owner/repo"
            );
        }
        return new ParsedFullName(owner, repo);
    }

    private record ParsedFullName(String owner, String repo) {}
}
