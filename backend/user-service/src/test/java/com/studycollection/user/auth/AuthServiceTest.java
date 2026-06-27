package com.studycollection.user.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceTest {
    @Test
    void demoAdminCanLogin() {
        AuthService service = new AuthService();

        LoginResponse response = service.login(new LoginRequest("admin", "admin123"));

        assertThat(response.token()).startsWith("demo-token-");
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.displayName()).isEqualTo("系统管理员");
    }
}
