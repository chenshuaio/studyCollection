package com.studycollection.user.auth;

public interface UserRepository {
    UserAccount findByUsername(String username);

    UserAccount findByDisplayName(String displayName);

    UserAccount save(UserAccount account);
}
