package com.committr.backend.user;

import com.committr.backend.crypto.EncryptionService;
import com.committr.backend.crypto.TokenEncryptionException;
import com.committr.backend.dto.github.GithubUserResponse;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public UserService(UserRepository userRepository, EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public User upsertUser(GithubUserResponse githubUser, String plaintextAccessToken) {
        LocalDateTime now = LocalDateTime.now();

        String username = githubUser.login();
        String avatarUrl = githubUser.avatarUrl() != null ? githubUser.avatarUrl() : "";
        String encryptedToken = encryptionService.encrypt(plaintextAccessToken);

        User saved = userRepository
            .findByGithubId(githubUser.id())
            .map(existing -> {
                existing.setUsername(username);
                existing.setAvatarUrl(avatarUrl);
                existing.setEncryptedAccessToken(encryptedToken);
                existing.setUpdatedAt(now);
                return userRepository.save(existing);
            })
            .orElseGet(() -> {
                User user = new User();
                user.setGithubId(githubUser.id());
                user.setUsername(username);
                user.setAvatarUrl(avatarUrl);
                user.setEncryptedAccessToken(encryptedToken);
                user.setCreatedAt(now);
                user.setUpdatedAt(now);
                return userRepository.save(user);
            });

        log.debug(
            "Stored encrypted GitHub token for githubId={} (ciphertext length={})",
            githubUser.id(),
            encryptedToken.length()
        );
        return saved;
    }

    public String getDecryptedAccessToken(User user) {
        if (user == null) {
            throw new TokenEncryptionException("User is null");
        }
        String stored = user.getEncryptedAccessToken();
        if (stored == null || stored.isBlank()) {
            throw new TokenEncryptionException("No encrypted token stored for user id=" + user.getId());
        }
        return encryptionService.decrypt(stored);
    }
}