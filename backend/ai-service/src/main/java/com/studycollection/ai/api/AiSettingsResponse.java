package com.studycollection.ai.api;

import com.studycollection.ai.app.AiModelSettings;

import java.time.Instant;

public record AiSettingsResponse(
        String provider,
        String endpoint,
        String modelName,
        boolean apiKeyConfigured,
        Long updatedBy,
        Instant updatedAt
) {
    public static AiSettingsResponse from(AiModelSettings settings, boolean apiKeyConfigured) {
        return new AiSettingsResponse(
                settings.provider(),
                settings.endpoint(),
                settings.modelName(),
                apiKeyConfigured,
                settings.updatedBy(),
                settings.updatedAt()
        );
    }
}
