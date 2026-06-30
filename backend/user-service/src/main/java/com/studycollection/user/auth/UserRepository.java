package com.studycollection.user.auth;

import java.util.List;

public interface UserRepository {
    UserAccount findByUsername(String username);

    UserAccount findByDisplayName(String displayName);

    List<UserAccount> findAll();

    UserAccount save(UserAccount account);
}
