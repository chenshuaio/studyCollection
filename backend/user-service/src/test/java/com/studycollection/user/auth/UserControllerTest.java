package com.studycollection.user.auth;

import com.studycollection.common.security.Role;
import com.studycollection.common.security.TokenService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerTest {
    @Test
    void listsUsersWithoutPasswordHashes() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        repository.save(new UserAccount(null, "admin", "{plain}admin123", "系统管理员", Role.ADMIN));
        repository.save(new UserAccount(null, "alice", "{plain}pass123456", "Alice", Role.USER));
        UserController controller = new UserController(new AuthService(repository, new TokenService("test-secret")));

        List<UserSummary> users = controller.listUsers().data();

        assertThat(users).extracting(UserSummary::username).containsExactly("admin", "alice");
        assertThat(users).extracting(UserSummary::displayName).contains("系统管理员", "Alice");
        assertThat(users).extracting(UserSummary::role).containsExactly("ADMIN", "USER");
        assertThat(users.toString()).doesNotContain("admin123", "pass123456", "{plain}");
    }
}
