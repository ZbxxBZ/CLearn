package com.clearn.api.auth;

import com.clearn.api.auth.dto.LoginRequest;
import com.clearn.api.auth.dto.LoginResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password";
    private static final String DUMMY_BCRYPT_HASH = "$2a$10$75AsrcqQNYrMUWKrpyGPqeSXSi8f79mdBr4M94ps.T4uAj9ISIuRe";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            throw badCredentials();
        }

        String username = request.username().trim();
        String password = request.password();
        UserAccount account = userMapper.findByUsername(username);
        if (account == null || !account.enabled()) {
            passwordEncoder.matches(password, DUMMY_BCRYPT_HASH);
            throw badCredentials();
        }

        if (!passwordEncoder.matches(password, account.passwordHash())) {
            throw badCredentials();
        }

        CurrentUser currentUser = new CurrentUser(account.id(), account.username(), account.role());
        String token = tokenService.createToken(currentUser);
        return new LoginResponse(token, account.username(), account.role().name());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BadCredentialsException badCredentials() {
        return new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
    }
}
