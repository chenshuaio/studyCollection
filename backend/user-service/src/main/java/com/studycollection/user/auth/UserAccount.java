package com.studycollection.user.auth;

import com.studycollection.common.security.Role;

public record UserAccount(
        Long id,
        String username,
        String passwordHash,
        String displayName,
        Role role
) {
}
