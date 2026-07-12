package com.studycollection.ai.app;

import java.net.URI;
import java.time.Clock;

public class AiSettingsService {
    public static final String PROVIDER = "OPENAI_COMPATIBLE";

    private final AiModelSettingsRepository repository;
    private final AiEnvironmentConfig environment;
    private final Clock clock;

    public AiSettingsService(AiModelSettingsRepository repository, AiEnvironmentConfig environment) {
        this(repository, environment, Clock.systemUTC());
    }

    public AiSettingsService(
            AiModelSettingsRepository repository,
            AiEnvironmentConfig environment,
            Clock clock
    ) {
        this.repository = repository;
        this.environment = environment;
        this.clock = clock;
    }

    public AiModelSettings current() {
        return repository.find().orElseGet(() -> new AiModelSettings(
                PROVIDER,
                environment.endpoint(),
                environment.modelName(),
                null,
                null
        ));
    }

    public AiModelSettings update(String endpoint, String modelName, Long updatedBy) {
        String normalizedEndpoint = validateEndpoint(endpoint);
        String normalizedModelName = modelName == null ? "" : modelName.trim();
        if (normalizedModelName.isBlank()) {
            throw new IllegalArgumentException("模型名称不能为空");
        }
        if (normalizedModelName.length() > 128) {
            throw new IllegalArgumentException("模型名称不能超过 128 个字符");
        }
        if (updatedBy == null) {
            throw new IllegalArgumentException("修改人不能为空");
        }
        return repository.save(new AiModelSettings(
                PROVIDER,
                normalizedEndpoint,
                normalizedModelName,
                updatedBy,
                clock.instant()
        ));
    }

    public boolean apiKeyConfigured() {
        return environment.apiKeyConfigured();
    }

    String apiKey() {
        return environment.apiKey();
    }

    private String validateEndpoint(String endpoint) {
        String normalized = endpoint == null ? "" : endpoint.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("在线模型端点不能为空");
        }
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("在线模型端点必须是合法的 http 或 https 地址");
        }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("在线模型端点仅支持 http 或 https 地址");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("在线模型端点必须包含有效主机");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("在线模型端点不能包含用户信息");
        }
        if (normalized.length() > 512) {
            throw new IllegalArgumentException("在线模型端点不能超过 512 个字符");
        }
        return normalized;
    }
}
