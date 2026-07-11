package com.studycollection.common.security;

public record AuthenticatedUser(Long userId, String username, Role role) {
    public static final String REQUEST_ATTRIBUTE = "studyCollection.authenticatedUser";
}
