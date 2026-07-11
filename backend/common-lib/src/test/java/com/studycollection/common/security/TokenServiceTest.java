package com.studycollection.common.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(claims).containsKey("expiresAt");
    }

    @Test
    void rejectsExpiredTokens() {
        Clock issuedAt = Clock.fixed(Instant.parse("2026-07-10T00:00:00Z"), ZoneOffset.UTC);
        TokenService issuer = new TokenService("local-secret", issuedAt, Duration.ofMinutes(30));
        String token = issuer.issue(10L, "alice", Role.USER);
        TokenService verifier = new TokenService(
                "local-secret",
                Clock.offset(issuedAt, Duration.ofMinutes(31)),
                Duration.ofMinutes(30)
        );

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("token 已过期");
    }

    @Test
    void escapesSpecialCharactersInsideUsernameClaim() {
        TokenService tokenService = new TokenService("local-secret");

        Map<String, String> claims = tokenService.verify(tokenService.issue(
                10L,
                "alice&userId=1&role=ADMIN",
                Role.USER
        ));

        assertThat(claims).containsEntry("userId", "10");
        assertThat(claims).containsEntry("username", "alice&userId=1&role=ADMIN");
        assertThat(claims).containsEntry("role", "USER");
    }
}
