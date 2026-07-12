package com.studycollection.ai.app;

public record AiEnvironmentConfig(
        String endpoint,
        String modelName,
        String apiKey
) {
    public AiEnvironmentConfig {
        endpoint = endpoint == null ? "" : endpoint.trim();
        modelName = modelName == null ? "" : modelName.trim();
        apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public boolean apiKeyConfigured() {
        return !apiKey.isBlank();
    }
}
