package com.clearn.api.auth;

import com.clearn.common.enums.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

@Service
public class TokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final Duration FUTURE_CLOCK_SKEW = Duration.ofSeconds(60);

    private final byte[] secret;
    private final Duration tokenTtl;
    private final Clock clock;

    @Autowired
    public TokenService(
            @Value("${clearn.security.token-secret}") String tokenSecret,
            @Value("${clearn.security.token-ttl}") Duration tokenTtl
    ) {
        this(tokenSecret, tokenTtl, Clock.systemUTC());
    }

    TokenService(String tokenSecret) {
        this(tokenSecret, Duration.ofHours(2), Clock.systemUTC());
    }

    TokenService(String tokenSecret, Duration tokenTtl, Clock clock) {
        if (tokenSecret == null || tokenSecret.isBlank()) {
            throw new IllegalArgumentException("clearn.security.token-secret must not be blank");
        }
        if (tokenTtl == null || tokenTtl.isZero() || tokenTtl.isNegative()) {
            throw new IllegalArgumentException("clearn.security.token-ttl must be positive");
        }
        this.secret = tokenSecret.getBytes(StandardCharsets.UTF_8);
        this.tokenTtl = tokenTtl;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public String createToken(CurrentUser user) {
        String payload = String.join(".",
                encode(user.id().toString()),
                encode(user.username()),
                encode(user.role().name()),
                Long.toString(Instant.now(clock).getEpochSecond())
        );
        return payload + "." + sign(payload);
    }

    public Optional<CurrentUser> parseToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String[] parts = token.split("\\.", -1);
        if (parts.length != 5) {
            return Optional.empty();
        }

        String payload = String.join(".", parts[0], parts[1], parts[2], parts[3]);
        if (!signatureMatches(sign(payload), parts[4])) {
            return Optional.empty();
        }

        try {
            Long userId = Long.valueOf(decode(parts[0]));
            String username = decode(parts[1]);
            UserRole role = UserRole.valueOf(decode(parts[2]));
            Instant issuedAt = Instant.ofEpochSecond(Long.parseLong(parts[3]));
            if (!issuedAtIsAllowed(issuedAt)) {
                return Optional.empty();
            }
            return Optional.of(new CurrentUser(userId, username, role));
        } catch (IllegalArgumentException | DateTimeException ex) {
            return Optional.empty();
        }
    }

    private boolean issuedAtIsAllowed(Instant issuedAt) {
        Instant now = Instant.now(clock);
        if (issuedAt.isAfter(now.plus(FUTURE_CLOCK_SKEW))) {
            return false;
        }
        return issuedAt.plus(tokenTtl).isAfter(now);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return ENCODER.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign token", ex);
        }
    }

    private boolean signatureMatches(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private String encode(String value) {
        return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(DECODER.decode(value), StandardCharsets.UTF_8);
    }
}
