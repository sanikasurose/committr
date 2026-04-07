package com.committr.backend.controller;

import com.committr.backend.dto.repository.AddRepositoryRequest;
import com.committr.backend.dto.repository.RepositoryResponse;
import com.committr.backend.repository.RepositoryService;
import com.committr.backend.session.SessionAuthenticationToken;
import com.committr.backend.session.SessionUserDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/repos")
public class RepositoryController {

    private final RepositoryService repositoryService;

    public RepositoryController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @GetMapping
    public List<RepositoryResponse> list(Authentication authentication) {
        Long userId = resolveSessionUser(authentication).id();
        return repositoryService
            .getUserRepositories(userId)
            .stream()
            .map(RepositoryResponse::from)
            .toList();
    }

    @PostMapping
    public ResponseEntity<RepositoryResponse> create(
        Authentication authentication,
        @Valid @RequestBody AddRepositoryRequest body
    ) {
        Long userId = resolveSessionUser(authentication).id();
        var saved = repositoryService.addRepository(body.fullName(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(RepositoryResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        Long userId = resolveSessionUser(authentication).id();
        repositoryService.deleteRepository(id, userId);
        return ResponseEntity.noContent().build();
    }

    private static SessionUserDto resolveSessionUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (authentication instanceof SessionAuthenticationToken sessionToken) {
            return (SessionUserDto) sessionToken.getPrincipal();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
}
