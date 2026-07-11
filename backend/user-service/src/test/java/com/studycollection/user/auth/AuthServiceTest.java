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

        assertThat(response.userId()).isPositive();
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.displayName()).isEqualTo("系统管理员");
        assertThat(tokenService.verify(response.token())).containsEntry("role", "ADMIN");
    }

    @Test
    void registeredUserCanLogin() {
        TokenService tokenService = new TokenService("test-secret");
        InMemoryUserRepository repository = new InMemoryUserRepository();
        AuthService service = new AuthService(repository, tokenService);

        RegisterResponse registered = service.register(new RegisterRequest("alice", "pass123456", "Alice"));
        LoginResponse loggedIn = service.login(new LoginRequest("alice", "pass123456"));

        assertThat(registered.username()).isEqualTo("alice");
        assertThat(registered.role()).isEqualTo(Role.USER.name());
        assertThat(registered.token()).isNotBlank();
        assertThat(repository.findByUsername("alice").passwordHash()).startsWith("{pbkdf2}");
        assertThat(repository.findByUsername("alice").passwordHash()).doesNotContain("pass123456");
        assertThat(tokenService.verify(registered.token())).containsEntry("userId", registered.userId().toString());
        assertThat(loggedIn.displayName()).isEqualTo("Alice");
    }

    @Test
    void successfulLoginUpgradesLegacyPlainPassword() {
        InMemoryUserRepository repository = InMemoryUserRepository.withDemoUsers();
        AuthService service = new AuthService(repository, new TokenService("test-secret"));

        service.login(new LoginRequest("user", "user123"));

        assertThat(repository.findByUsername("user").passwordHash()).startsWith("{pbkdf2}");
    }

    @Test
    void duplicateUsernameIsRejected() {
        AuthService service = new AuthService(new InMemoryUserRepository(), new TokenService("test-secret"));

        service.register(new RegisterRequest("alice", "pass123456", "Alice"));

        assertThatThrownBy(() -> service.register(new RegisterRequest("alice", "another123", "Alice 2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用户名已存在");
    }

    @Test
    void duplicateDisplayNameIsRejected() {
        AuthService service = new AuthService(new InMemoryUserRepository(), new TokenService("test-secret"));

        service.register(new RegisterRequest("alice", "pass123456", "同名用户"));

        assertThatThrownBy(() -> service.register(new RegisterRequest("bob", "another123", "同名用户")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("昵称已存在");
    }
}
