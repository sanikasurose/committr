package com.committr.backend.error;

import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REPOS_USER_GITHUB_REPO_UNIQUE = "repositories_user_github_repo_unique";

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        if (!isRepositoriesUserGithubRepoDuplicate(ex)) {
            throw ex;
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Repository already added"));
    }

    private static boolean isRepositoriesUserGithubRepoDuplicate(DataIntegrityViolationException ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException hibernateCv
                && REPOS_USER_GITHUB_REPO_UNIQUE.equals(hibernateCv.getConstraintName())) {
                return true;
            }
            String msg = t.getMessage();
            if (msg != null && msg.contains(REPOS_USER_GITHUB_REPO_UNIQUE)) {
                return true;
            }
        }
        return false;
    }
}
