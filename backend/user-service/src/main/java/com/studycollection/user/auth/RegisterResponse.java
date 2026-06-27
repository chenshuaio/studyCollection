package com.studycollection.user.auth;

public record RegisterResponse(Long userId, String username, String displayName, String role) {
}
