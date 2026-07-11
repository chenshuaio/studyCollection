package com.studycollection.user.auth;

public record RegisterResponse(
        String token,
        Long userId,
        String username,
        String displayName,
        String role
) {
}
