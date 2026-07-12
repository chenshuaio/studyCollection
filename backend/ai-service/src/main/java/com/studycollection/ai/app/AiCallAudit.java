package com.studycollection.ai.app;

import java.time.Instant;

public record AiCallAudit(
        Long id,
        Long userId,
        String purpose,
        String provider,
        String modelName,
        String status,
        String failureReason,
        long durationMs,
        Instant createdAt
) {
}
