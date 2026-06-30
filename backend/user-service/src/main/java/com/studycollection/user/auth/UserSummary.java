package com.studycollection.user.auth;

public record UserSummary(
        Long id,
        String username,
        String displayName,
        String role
) {
}
