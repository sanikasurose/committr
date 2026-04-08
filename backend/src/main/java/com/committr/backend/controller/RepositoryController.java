package com.committr.backend.controller;

import com.committr.backend.dto.repository.AddRepositoryRequest;
import com.committr.backend.dto.repository.RepositoryResponse;
import com.committr.backend.repository.RepositoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repos")
public class RepositoryController {

    private final RepositoryService repositoryService;

    public RepositoryController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @GetMapping
    public List<RepositoryResponse> list() {
        return repositoryService
            .getUserRepositories()
            .stream()
            .map(RepositoryResponse::from)
            .toList();
    }

    @PostMapping
    public ResponseEntity<RepositoryResponse> create(@Valid @RequestBody AddRepositoryRequest body) {
        var saved = repositoryService.addRepository(body.fullName());
        return ResponseEntity.status(HttpStatus.CREATED).body(RepositoryResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repositoryService.deleteRepository(id);
        return ResponseEntity.noContent().build();
    }
}
