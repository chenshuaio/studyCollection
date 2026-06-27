package com.studycollection.common.security;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {
    @Test
    void signsAndVerifiesUserToken() {
        TokenService tokenService = new TokenService("local-secret");

        String token = tokenService.issue(10L, "alice", Role.USER);
        Map<String, String> claims = tokenService.verify(token);

        assertThat(token).contains(".");
        assertThat(claims).containsEntry("userId", "10");
        assertThat(claims).containsEntry("username", "alice");
        assertThat(claims).containsEntry("role", "USER");
    }
}
