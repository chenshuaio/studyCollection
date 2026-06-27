package com.studycollection.user.auth;

public record LoginResponse(String token, String role, String displayName) {
}
