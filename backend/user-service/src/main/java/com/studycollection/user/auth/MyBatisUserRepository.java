package com.studycollection.user.auth;

import com.studycollection.common.security.Role;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Primary
@Repository
@Profile("local-mysql")
public class MyBatisUserRepository implements UserRepository {
    private final UserMapper userMapper;

    public MyBatisUserRepository(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserAccount findByUsername(String username) {
        UserEntity entity = userMapper.findByUsername(username);
        return entity == null ? null : toAccount(entity);
    }

    @Override
    public UserAccount findByDisplayName(String displayName) {
        UserEntity entity = userMapper.findByDisplayName(displayName);
        return entity == null ? null : toAccount(entity);
    }

    @Override
    public List<UserAccount> findAll() {
        return userMapper.findAll().stream()
                .map(this::toAccount)
                .toList();
    }

    @Override
    public UserAccount save(UserAccount account) {
        UserEntity entity = toEntity(account);
        userMapper.insert(entity);
        return toAccount(entity);
    }

    private UserEntity toEntity(UserAccount account) {
        UserEntity entity = new UserEntity();
        entity.setId(account.id());
        entity.setUsername(account.username());
        entity.setPasswordHash(account.passwordHash());
        entity.setDisplayName(account.displayName());
        entity.setRole(account.role().name());
        return entity;
    }

    private UserAccount toAccount(UserEntity entity) {
        return new UserAccount(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getDisplayName(),
                Role.valueOf(entity.getRole())
        );
    }
}
