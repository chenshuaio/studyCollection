package com.studycollection.user.auth;

import com.studycollection.common.security.Role;
import com.studycollection.common.security.TokenService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {
    @Test
    void demoAdminCanLoginWithSignedToken() {
        TokenService tokenService = new TokenService("test-secret");
        AuthService service = new AuthService(InMemoryUserRepository.withDemoUsers(), tokenService);

        LoginResponse response = service.login(new LoginRequest("admin", "admin123"));

        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.displayName()).isEqualTo("系统管理员");
        assertThat(tokenService.verify(response.token())).containsEntry("role", "ADMIN");
    }

    @Test
    void registeredUserCanLogin() {
        TokenService tokenService = new TokenService("test-secret");
        AuthService service = new AuthService(new InMemoryUserRepository(), tokenService);

        RegisterResponse registered = service.register(new RegisterRequest("alice", "pass123456", "Alice"));
        LoginResponse loggedIn = service.login(new LoginRequest("alice", "pass123456"));

        assertThat(registered.username()).isEqualTo("alice");
        assertThat(registered.role()).isEqualTo(Role.USER.name());
        assertThat(loggedIn.displayName()).isEqualTo("Alice");
    }

    @Test
    void duplicateUsernameIsRejected() {
        AuthService service = new AuthService(new InMemoryUserRepository(), new TokenService("test-secret"));

        service.register(new RegisterRequest("alice", "pass123456", "Alice"));

        assertThatThrownBy(() -> service.register(new RegisterRequest("alice", "another123", "Alice 2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用户名已存在");
    }
}
