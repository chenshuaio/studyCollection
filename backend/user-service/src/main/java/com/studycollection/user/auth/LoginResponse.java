package com.studycollection.user.auth;

public record LoginResponse(
        String token,
        Long userId,
        String username,
        String role,
        String displayName
) {
}
