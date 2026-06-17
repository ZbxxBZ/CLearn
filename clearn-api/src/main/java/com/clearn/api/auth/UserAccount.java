package com.clearn.api.auth;

import com.clearn.common.enums.UserRole;

public record UserAccount(
        Long id,
        String username,
        String passwordHash,
        UserRole role,
        boolean enabled
) {
}
