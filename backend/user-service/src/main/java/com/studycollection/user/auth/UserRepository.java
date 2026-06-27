package com.studycollection.user.auth;

public interface UserRepository {
    UserAccount findByUsername(String username);

    UserAccount save(UserAccount account);
}
