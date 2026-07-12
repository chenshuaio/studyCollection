package com.studycollection.ai.app;

import java.time.Instant;

public record AiModelSettings(
        String provider,
        String endpoint,
        String modelName,
        Long updatedBy,
        Instant updatedAt
) {
}
