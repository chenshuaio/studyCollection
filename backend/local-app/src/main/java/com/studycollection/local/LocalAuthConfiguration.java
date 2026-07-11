package com.studycollection.local;

import com.studycollection.common.security.TokenService;
import com.studycollection.user.auth.InMemoryUserRepository;
import com.studycollection.user.auth.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;

import java.security.SecureRandom;
import java.util.Base64;

@Configuration
public class LocalAuthConfiguration {
    @Bean
    public TokenService tokenService(@Value("${STUDY_COLLECTION_TOKEN_SECRET:}") String configuredSecret) {
        String secret = configuredSecret == null || configuredSecret.isBlank()
                ? generateSecret()
                : configuredSecret;
        return new TokenService(secret);
    }

    @Bean
    @Profile("!local-mysql")
    public UserRepository localUserRepository() {
        return InMemoryUserRepository.withDemoUsers();
    }

    private String generateSecret() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }
}
