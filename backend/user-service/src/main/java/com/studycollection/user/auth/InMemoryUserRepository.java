package com.studycollection.user.auth;

import com.studycollection.common.security.Role;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryUserRepository implements UserRepository {
    private final AtomicLong ids = new AtomicLong(1);
    private final Map<String, UserAccount> accounts = new LinkedHashMap<>();

    public static InMemoryUserRepository withDemoUsers() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        repository.save(new UserAccount(null, "admin", "{plain}admin123", "系统管理员", Role.ADMIN));
        repository.save(new UserAccount(null, "user", "{plain}user123", "学习用户", Role.USER));
        return repository;
    }

    @Override
    public UserAccount findByUsername(String username) {
        return accounts.get(username);
    }

    @Override
    public UserAccount findByDisplayName(String displayName) {
        return accounts.values().stream()
                .filter(account -> account.displayName().equals(displayName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<UserAccount> findAll() {
        return List.copyOf(accounts.values());
    }

    @Override
    public UserAccount save(UserAccount account) {
        Long id = account.id() == null ? ids.getAndIncrement() : account.id();
        UserAccount saved = new UserAccount(
                id,
                account.username(),
                account.passwordHash(),
                account.displayName(),
                account.role()
        );
        accounts.put(saved.username(), saved);
        return saved;
    }
}
