package com.studycollection.ai.app;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

public class AiAnalysisService {
    private final OnlineModelClient onlineModelClient;
    private final AiCallAuditRepository auditRepository;
    private final Clock clock;

    public AiAnalysisService() {
        this(new HttpOnlineModelClient(), new InMemoryAiCallAuditRepository(), Clock.systemUTC());
    }

    public AiAnalysisService(OnlineModelClient onlineModelClient) {
        this(onlineModelClient, new InMemoryAiCallAuditRepository(), Clock.systemUTC());
    }

    public AiAnalysisService(
            OnlineModelClient onlineModelClient,
            AiCallAuditRepository auditRepository,
            Clock clock
    ) {
        this.onlineModelClient = onlineModelClient;
        this.auditRepository = auditRepository;
        this.clock = clock;
    }

    public AnalysisAdvice generate(AnalysisMode mode, String summary) {
        return generate(mode, null, "UNKNOWN", summary);
    }

    public AnalysisAdvice generate(
            AnalysisMode mode,
            Long userId,
            String purpose,
            String summary
    ) {
        if (mode == AnalysisMode.ONLINE_MODEL) {
            long startedAt = System.nanoTime();
            try {
                String advice = onlineModelClient.generateAdvice(summary);
                if (advice != null && !advice.isBlank()) {
                    saveAudit(userId, purpose, "SUCCESS", null, startedAt);
                    return new AnalysisAdvice("ONLINE_MODEL", advice);
                }
            } catch (RuntimeException exception) {
                saveAudit(userId, purpose, "FALLBACK", sanitize(exception), startedAt);
                return fallbackAdvice(summary, "在线模型暂不可用，已自动切换为规则分析。");
            }
            saveAudit(userId, purpose, "FALLBACK", "在线模型返回为空", startedAt);
            return fallbackAdvice(summary, "在线模型返回为空，已自动切换为规则分析。");
        }
        return fallbackAdvice(summary, "规则分析建议：");
    }

    private void saveAudit(
            Long userId,
            String purpose,
            String status,
            String failureReason,
            long startedAt
    ) {
        try {
            auditRepository.save(new AiCallAudit(
                    null,
                    userId,
                    purpose == null || purpose.isBlank() ? "UNKNOWN" : purpose.trim(),
                    readProvider(),
                    readModel(),
                    status,
                    failureReason,
                    Math.max(0, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)),
                    clock.instant()
            ));
        } catch (RuntimeException ignored) {
            // Audit storage must never prevent the required rules fallback.
        }
    }

    private String readProvider() {
        try {
            String provider = onlineModelClient.provider();
            return provider == null || provider.isBlank() ? "UNKNOWN" : provider.trim();
        } catch (RuntimeException ignored) {
            return "UNKNOWN";
        }
    }

    private String readModel() {
        try {
            String model = onlineModelClient.model();
            return model == null ? "" : model.trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private String sanitize(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        String sanitized = message
                .replaceAll("(?i)Bearer\\s+\\S+", "Bearer [REDACTED]")
                .replaceAll("[\\r\\n\\t]+", " ")
                .trim();
        return sanitized.length() <= 240 ? sanitized : sanitized.substring(0, 240);
    }

    private AnalysisAdvice fallbackAdvice(String summary, String prefix) {
        return new AnalysisAdvice("RULES", prefix + "请针对薄弱项继续练习。" + summary);
    }
}
