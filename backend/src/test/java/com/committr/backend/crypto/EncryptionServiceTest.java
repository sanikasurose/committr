package com.committr.backend.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.committr.backend.config.SecurityProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class EncryptionServiceTest {

    @Test
    void roundTrip_utf8Key() {
        byte[] key = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        assertThat(key.length).isEqualTo(32);
        EncryptionService svc = new EncryptionService(new SecurityProperties(new String(key, StandardCharsets.UTF_8)));
        String plain = "gho_exampleGitHubTokenValue";
        assertThat(svc.decrypt(svc.encrypt(plain))).isEqualTo(plain);
    }

    @Test
    void roundTrip_base64Key() {
        byte[] raw = new byte[32];
        for (int i = 0; i < 32; i++) {
            raw[i] = (byte) i;
        }
        String b64 = Base64.getEncoder().encodeToString(raw);
        EncryptionService svc = new EncryptionService(new SecurityProperties(b64));
        String plain = "token-with-unicode-αβγ";
        assertThat(svc.decrypt(svc.encrypt(plain))).isEqualTo(plain);
    }

    @Test
    void decrypt_wrongKey_fails() {
        EncryptionService enc = new EncryptionService(new SecurityProperties("0123456789abcdef0123456789abcdef"));
        String ciphertext = enc.encrypt("secret");
        EncryptionService other = new EncryptionService(new SecurityProperties("fedcba9876543210fedcba9876543210"));
        assertThatThrownBy(() -> other.decrypt(ciphertext))
            .isInstanceOf(TokenEncryptionException.class)
            .hasMessageContaining("Decryption failed");
    }

    @Test
    void requireKeyBytes_rejectsShortKey() {
        assertThatThrownBy(() -> new EncryptionService(new SecurityProperties("tooshort")))
            .isInstanceOf(IllegalStateException.class);
    }
}
