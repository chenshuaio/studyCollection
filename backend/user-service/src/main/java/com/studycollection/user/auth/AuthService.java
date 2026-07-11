package com.studycollection.user.auth;

import com.studycollection.common.security.Role;
import com.studycollection.common.security.TokenService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordHasher passwordHasher = new PasswordHasher();

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
        if (request.displayName() == null || request.displayName().isBlank()) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        if (userRepository.findByDisplayName(request.displayName()) != null) {
            throw new IllegalArgumentException("昵称已存在");
        }
        UserAccount account = userRepository.save(new UserAccount(
                null,
                request.username(),
                passwordHasher.hash(request.password()),
                request.displayName(),
                Role.USER
        ));
        String token = tokenService.issue(account.id(), account.username(), account.role());
        return new RegisterResponse(
                token,
                account.id(),
                account.username(),
                account.displayName(),
                account.role().name()
        );
    }

    public LoginResponse login(LoginRequest request) {
        UserAccount account = userRepository.findByUsername(request.username());
        if (account == null || !passwordHasher.matches(request.password(), account.passwordHash())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        if (passwordHasher.needsUpgrade(account.passwordHash())) {
            userRepository.updatePasswordHash(account.id(), passwordHasher.hash(request.password()));
        }
        String token = tokenService.issue(account.id(), account.username(), account.role());
        return new LoginResponse(
                token,
                account.id(),
                account.username(),
                account.role().name(),
                account.displayName()
        );
    }

    public List<UserSummary> listUsers() {
        return userRepository.findAll().stream()
                .map(account -> new UserSummary(
                        account.id(),
                        account.username(),
                        account.displayName(),
                        account.role().name()
                ))
                .toList();
    }
}
