package com.studycollection.user.auth;

import com.studycollection.common.security.Role;
import com.studycollection.common.security.TokenService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    public RegisterResponse register(RegisterRequest request) {
        if (request.username() == null || request.username().isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (request.password() == null || request.password().length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
        if (userRepository.findByUsername(request.username()) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        UserAccount account = userRepository.save(new UserAccount(
                null,
                request.username(),
                "{plain}" + request.password(),
                request.displayName(),
                Role.USER
        ));
        return new RegisterResponse(account.id(), account.username(), account.displayName(), account.role().name());
    }

    public LoginResponse login(LoginRequest request) {
        UserAccount account = userRepository.findByUsername(request.username());
        if (account == null || !account.passwordHash().equals("{plain}" + request.password())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = tokenService.issue(account.id(), account.username(), account.role());
        return new LoginResponse(token, account.role().name(), account.displayName());
    }
}
