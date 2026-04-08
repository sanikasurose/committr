package com.committr.backend.repository;

import com.committr.backend.dto.github.GitHubRepoResponse;
import com.committr.backend.github.GitHubClient;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final GitHubClient gitHubClient;

    public RepositoryService(RepositoryRepository repositoryRepository, GitHubClient gitHubClient) {
        this.repositoryRepository = repositoryRepository;
        this.gitHubClient = gitHubClient;
    }

    @Transactional(readOnly = true)
    public List<RepositoryEntity> getUserRepositories(Long userId) {
        return repositoryRepository.findByUserIdAndDeletedAtIsNull(userId);
    }

    @Transactional
    public RepositoryEntity addRepository(String fullName, Long userId) {
        String trimmed = fullName == null ? "" : fullName.trim();
        ParsedFullName parsed = parseFullName(trimmed);

        GitHubRepoResponse github = gitHubClient.getRepository(parsed.owner(), parsed.repo());
        long githubRepoId = github.id();
        if (repositoryRepository.existsByUserIdAndGithubRepoId(userId, githubRepoId)) {
            RepositoryEntity existing = repositoryRepository
                .findByUserIdAndGithubRepoId(userId, githubRepoId)
                .orElseThrow(() -> new IllegalStateException("Repository row missing after exists check"));
            if (existing.getDeletedAt() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Repository already added");
            }
            applyGithubFields(existing, github);
            existing.setDeletedAt(null);
            return repositoryRepository.save(existing);
        }

        RepositoryEntity entity = new RepositoryEntity();
        entity.setUserId(userId);
        applyGithubFields(entity, github);
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

    private static void applyGithubFields(RepositoryEntity target, GitHubRepoResponse github) {
        target.setGithubRepoId(github.id());
        target.setName(github.name());
        target.setFullName(github.fullName());
        target.setOwnerLogin(github.owner().login());
        target.setPrivate(github.isPrivate());
        target.setHtmlUrl(github.htmlUrl());
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
