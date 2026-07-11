package com.studycollection.local;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studycollection.common.api.ApiResponse;
import com.studycollection.common.api.ErrorCode;
import com.studycollection.common.security.AdminOnly;
import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.common.security.Role;
import com.studycollection.common.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class LocalAuthenticationInterceptor implements HandlerInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    public LocalAuthenticationInterceptor(TokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        AuthenticatedUser currentUser = authenticate(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (currentUser == null) {
            writeFailure(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
            return false;
        }
        request.setAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, currentUser);

        if (requiresAdministrator(handlerMethod) && currentUser.role() != Role.ADMIN) {
            writeFailure(response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
            return false;
        }
        return true;
    }

    private AuthenticatedUser authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        try {
            Map<String, String> claims = tokenService.verify(authorization.substring(BEARER_PREFIX.length()).trim());
            Long userId = Long.valueOf(claims.get("userId"));
            String username = claims.get("username");
            Role role = Role.valueOf(claims.get("role"));
            if (userId <= 0 || username == null || username.isBlank()) {
                return null;
            }
            return new AuthenticatedUser(userId, username, role);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean requiresAdministrator(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), AdminOnly.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), AdminOnly.class);
    }

    private void writeFailure(HttpServletResponse response, HttpStatus status, ErrorCode errorCode) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(errorCode));
    }
}
