package com.studycollection.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TokenService {
    private final String secret;

    public TokenService(String secret) {
        this.secret = secret;
    }

    public String issue(Long userId, String username, Role role) {
        String payload = "userId=" + userId + "&username=" + username + "&role=" + role.name();
        String encodedPayload = encode(payload);
        return encodedPayload + "." + sign(encodedPayload);
    }

    public Map<String, String> verify(String token) {
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2 || !sign(parts[0]).equals(parts[1])) {
            throw new IllegalArgumentException("无效 token");
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        return parse(payload);
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

    private Map<String, String> parse(String payload) {
        return payload.lines()
                .flatMap(line -> java.util.Arrays.stream(line.split("&")))
                .map(item -> item.split("=", 2))
                .collect(Collectors.toMap(
                        item -> item[0],
                        item -> item.length > 1 ? item[1] : "",
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }
}
