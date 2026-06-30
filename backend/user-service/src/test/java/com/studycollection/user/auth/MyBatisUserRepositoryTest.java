package com.studycollection.user.auth;

import com.studycollection.common.security.Role;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MyBatisUserRepositoryTest {
    @Test
    void savesFindsAndListsUserAccountsThroughMapper() {
        FakeUserMapper mapper = new FakeUserMapper();
        MyBatisUserRepository repository = new MyBatisUserRepository(mapper);

        UserAccount saved = repository.save(new UserAccount(
                null,
                "mysql-user",
                "{plain}pass123456",
                "MySQL 用户",
                Role.USER
        ));

        UserAccount found = repository.findByUsername("mysql-user");
        UserAccount foundByDisplayName = repository.findByDisplayName("MySQL 用户");
        List<UserAccount> users = repository.findAll();

        assertThat(saved.id()).isEqualTo(1L);
        assertThat(found.displayName()).isEqualTo("MySQL 用户");
        assertThat(found.role()).isEqualTo(Role.USER);
        assertThat(foundByDisplayName.username()).isEqualTo("mysql-user");
        assertThat(users).extracting(UserAccount::username).containsExactly("mysql-user");
    }

    private static class FakeUserMapper implements UserMapper {
        private final Map<String, UserEntity> users = new HashMap<>();
        private long nextId = 1L;

        @Override
        public UserEntity findByUsername(String username) {
            return users.get(username);
        }

        @Override
        public UserEntity findByDisplayName(String displayName) {
            return users.values().stream()
                    .filter(user -> user.getDisplayName().equals(displayName))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<UserEntity> findAll() {
            return List.copyOf(users.values());
        }

        @Override
        public int insert(UserEntity entity) {
            entity.setId(nextId++);
            users.put(entity.getUsername(), entity);
            return 1;
        }
    }
}
