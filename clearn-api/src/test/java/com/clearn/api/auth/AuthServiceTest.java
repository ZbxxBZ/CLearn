package com.clearn.api.auth;

import com.clearn.api.auth.dto.LoginRequest;
import com.clearn.common.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    @Test
    void missingUserStillRunsDummyPasswordCheck() {
        TrackingPasswordEncoder passwordEncoder = new TrackingPasswordEncoder();
        AuthService authService = new AuthService(new FixedUserMapper(null), passwordEncoder, new TokenService("secret"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing", "password")))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(passwordEncoder.matchesCalls).isEqualTo(1);
    }

    @Test
    void disabledUserStillRunsDummyPasswordCheck() {
        TrackingPasswordEncoder passwordEncoder = new TrackingPasswordEncoder();
        UserAccount disabled = new UserAccount(10L, "disabled", "unused", UserRole.STUDENT, false);
        AuthService authService = new AuthService(new FixedUserMapper(disabled), passwordEncoder, new TokenService("secret"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("disabled", "password")))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(passwordEncoder.matchesCalls).isEqualTo(1);
    }

    @Test
    void nullRequestThrowsBadCredentials() {
        AuthService authService = new AuthService(new FixedUserMapper(null), new TrackingPasswordEncoder(), new TokenService("secret"));

        assertThatThrownBy(() -> authService.login(null))
                .isInstanceOf(BadCredentialsException.class);
    }

    private static final class FixedUserMapper implements UserMapper {
        private final UserAccount account;

        private FixedUserMapper(UserAccount account) {
            this.account = account;
        }

        @Override
        public UserAccount findByUsername(String username) {
            return account;
        }

        @Override
        public UserAccount findById(Long id) {
            return account;
        }
    }

    private static final class TrackingPasswordEncoder implements PasswordEncoder {
        private int matchesCalls;

        @Override
        public String encode(CharSequence rawPassword) {
            return rawPassword.toString();
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            matchesCalls++;
            return false;
        }
    }
}
