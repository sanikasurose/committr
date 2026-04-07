package com.committr.backend.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public record SecurityProperties(String encryptionKey) {

    public SecurityProperties {
        decodeKeyMaterialToBytes(encryptionKey);
    }

    public byte[] requireKeyBytes() {
        return decodeKeyMaterialToBytes(encryptionKey);
    }

    private static byte[] decodeKeyMaterialToBytes(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                "ENCRYPTION_KEY is missing: set security.encryption-key / ENCRYPTION_KEY (32 bytes for AES-256)."
            );
        }
        String trimmed = key.strip();
        byte[] utf8 = trimmed.getBytes(StandardCharsets.UTF_8);
        if (utf8.length == 32) {
            return utf8;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(trimmed);
            if (decoded.length == 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // treat as invalid key material below
        }
        throw new IllegalStateException(
            "ENCRYPTION_KEY must be exactly 32 UTF-8 bytes or a Base64 string that decodes to 32 bytes (AES-256)."
        );
    }
}
