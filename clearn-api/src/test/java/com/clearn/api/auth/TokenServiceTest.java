package com.clearn.api.auth;

import com.clearn.common.enums.UserRole;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {
    private static final String SECRET = "unit-test-token-secret";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Instant NOW = Instant.parse("2026-06-16T00:00:00Z");

    private final TokenService tokenService = new TokenService(SECRET, Duration.ofHours(2), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void parsesValidToken() {
        CurrentUser user = new CurrentUser(42L, "student", UserRole.STUDENT);

        CurrentUser parsed = tokenService.parseToken(tokenService.createToken(user)).orElseThrow();

        assertThat(parsed).isEqualTo(user);
    }

    @Test
    void rejectsExpiredToken() {
        String token = signedToken(42L, "student", UserRole.STUDENT, NOW.minusSeconds(3 * 60 * 60));

        assertThat(tokenService.parseToken(token)).isEmpty();
    }

    @Test
    void rejectsFutureToken() {
        String token = signedToken(42L, "student", UserRole.STUDENT, NOW.plusSeconds(5 * 60));

        assertThat(tokenService.parseToken(token)).isEmpty();
    }

    @Test
    void rejectsTamperedToken() {
        String token = tokenService.createToken(new CurrentUser(42L, "student", UserRole.STUDENT));
        String tampered = token.replace("c3R1ZGVudA", "YWRtaW4");

        assertThat(tokenService.parseToken(tampered)).isEmpty();
    }

    private String signedToken(Long userId, String username, UserRole role, Instant issuedAt) {
        String payload = String.join(".",
                encode(userId.toString()),
                encode(username),
                encode(role.name()),
                Long.toString(issuedAt.getEpochSecond())
        );
        return payload + "." + sign(payload);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return ENCODER.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String encode(String value) {
        return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
