package com.clearn.api.auth;

import com.clearn.common.enums.UserRole;

public record CurrentUser(Long id, String username, UserRole role) {
}
