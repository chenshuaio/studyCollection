package com.studycollection.user.auth;

public class AuthService {
    public LoginResponse login(LoginRequest request) {
        if ("admin".equals(request.username()) && "admin123".equals(request.password())) {
            return new LoginResponse("demo-token-admin", "ADMIN", "系统管理员");
        }
        if ("user".equals(request.username()) && "user123".equals(request.password())) {
            return new LoginResponse("demo-token-user", "USER", "学习用户");
        }
        throw new IllegalArgumentException("用户名或密码错误");
    }
}
