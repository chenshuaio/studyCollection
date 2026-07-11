package com.studycollection.user.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHasherTest {
    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void hashesPasswordsWithSaltAndVerifiesThem() {
        String first = passwordHasher.hash("pass123456");
        String second = passwordHasher.hash("pass123456");

        assertThat(first).startsWith("{pbkdf2}");
        assertThat(first).doesNotContain("pass123456");
        assertThat(second).isNotEqualTo(first);
        assertThat(passwordHasher.matches("pass123456", first)).isTrue();
        assertThat(passwordHasher.matches("wrong-password", first)).isFalse();
    }

    @Test
    void stillVerifiesLegacyPlainPasswordsForMigration() {
        assertThat(passwordHasher.matches("user123", "{plain}user123")).isTrue();
        assertThat(passwordHasher.needsUpgrade("{plain}user123")).isTrue();
    }
}
