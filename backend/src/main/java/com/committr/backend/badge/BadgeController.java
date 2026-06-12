package com.committr.backend.badge;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/badge")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    @GetMapping(value = "/{username}", produces = "image/svg+xml")
    public ResponseEntity<String> badge(@PathVariable String username) {
        String svg = badgeService.getBadgeSvg(username);
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=900")
            .header(HttpHeaders.CONTENT_TYPE, "image/svg+xml")
            .body(svg);
    }
}
