package com.committr.backend.user;

import com.committr.backend.dto.github.GithubUserResponse;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User upsertUser(GithubUserResponse githubUser, String accessToken) {
        LocalDateTime now = LocalDateTime.now();

        String username = githubUser.login();
        String avatarUrl = githubUser.avatarUrl() != null ? githubUser.avatarUrl() : "";

        return userRepository
            .findByGithubId(githubUser.id())
            .map(existing -> {
                existing.setUsername(username);
                existing.setAvatarUrl(avatarUrl);
                existing.setEncryptedAccessToken(accessToken);
                existing.setUpdatedAt(now);
                return userRepository.save(existing);
            })
            .orElseGet(() -> {
                User user = new User();
                user.setGithubId(githubUser.id());
                user.setUsername(username);
                user.setAvatarUrl(avatarUrl);
                user.setEncryptedAccessToken(accessToken);
                user.setCreatedAt(now);
                user.setUpdatedAt(now);
                return userRepository.save(user);
            });
    }
}