package com.studycollection.local;

import com.studycollection.common.security.TokenService;
import com.studycollection.user.auth.InMemoryUserRepository;
import com.studycollection.user.auth.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class LocalAuthConfiguration {
    @Bean
    public TokenService tokenService() {
        return new TokenService("study-collection-local-secret");
    }

    @Bean
    @Profile("!local-mysql")
    public UserRepository localUserRepository() {
        return InMemoryUserRepository.withDemoUsers();
    }
}
