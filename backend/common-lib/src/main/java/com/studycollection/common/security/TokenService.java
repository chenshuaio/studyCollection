package com.studycollection.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TokenService {
    private final String secret;
    private final Clock clock;
    private final Duration tokenTtl;

    public TokenService(String secret) {
        this(secret, Clock.systemUTC(), Duration.ofHours(12));
    }

    public TokenService(String secret, Clock clock, Duration tokenTtl) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("token 密钥不能为空");
        }
        if (tokenTtl == null || tokenTtl.isZero() || tokenTtl.isNegative()) {
            throw new IllegalArgumentException("token 有效期必须大于 0");
        }
        this.secret = secret;
        this.clock = clock;
        this.tokenTtl = tokenTtl;
    }

    public String issue(Long userId, String username, Role role) {
        long expiresAt = Instant.now(clock).plus(tokenTtl).getEpochSecond();
        String payload = "userId=" + userId
                + "&username=" + encodeClaim(username)
                + "&role=" + role.name()
                + "&expiresAt=" + expiresAt;
        String encodedPayload = encode(payload);
        return encodedPayload + "." + sign(encodedPayload);
    }

    public Map<String, String> verify(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("无效 token");
        }
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2 || !constantTimeEquals(sign(parts[0]), parts[1])) {
            throw new IllegalArgumentException("无效 token");
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        Map<String, String> claims = parse(payload);
        long expiresAt;
        try {
            expiresAt = Long.parseLong(claims.get("expiresAt"));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("无效 token", exception);
        }
        if (Instant.now(clock).getEpochSecond() >= expiresAt) {
            throw new IllegalArgumentException("token 已过期");
        }
        return claims;
    }

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("token 签名失败", exception);
        }
    }

    private String encode(String payload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String encodeClaim(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private Map<String, String> parse(String payload) {
        return payload.lines()
                .flatMap(line -> java.util.Arrays.stream(line.split("&")))
                .map(item -> item.split("=", 2))
                .collect(Collectors.toMap(
                        item -> item[0],
                        item -> item.length > 1 ? URLDecoder.decode(item[1], StandardCharsets.UTF_8) : "",
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }
}
