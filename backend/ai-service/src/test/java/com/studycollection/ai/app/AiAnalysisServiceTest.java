package com.studycollection.ai.app;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AiAnalysisServiceTest {
    @Test
    void offlineModeUsesRuleAdvice() {
        AiAnalysisService service = new AiAnalysisService();

        AnalysisAdvice advice = service.generate(AnalysisMode.OFFLINE_RULES, "集合框架正确率 50%");

        assertThat(advice.source()).isEqualTo("RULES");
        assertThat(advice.content()).contains("集合框架");
    }

    @Test
    void onlineModeUsesConfiguredModelClient() {
        AiAnalysisService service = new AiAnalysisService(summary -> "在线建议：" + summary);

        AnalysisAdvice advice = service.generate(AnalysisMode.ONLINE_MODEL, "JVM 正确率 20%");

        assertThat(advice.source()).isEqualTo("ONLINE_MODEL");
        assertThat(advice.content()).isEqualTo("在线建议：JVM 正确率 20%");
    }

    @Test
    void onlineModeFallsBackToRulesWhenModelFails() {
        AiAnalysisService service = new AiAnalysisService(summary -> {
            throw new IllegalStateException("模型暂不可用");
        });

        AnalysisAdvice advice = service.generate(AnalysisMode.ONLINE_MODEL, "并发编程正确率 30%");

        assertThat(advice.source()).isEqualTo("RULES");
        assertThat(advice.content()).contains("在线模型暂不可用");
        assertThat(advice.content()).contains("并发编程");
    }

    @Test
    void onlineSuccessWritesAuditWithUserPurposeAndModelMetadata() {
        InMemoryAiCallAuditRepository audits = new InMemoryAiCallAuditRepository();
        OnlineModelClient client = new OnlineModelClient() {
            @Override
            public String generateAdvice(String summary) {
                return "在线建议：" + summary;
            }

            @Override
            public String provider() {
                return "OPENAI_COMPATIBLE";
            }

            @Override
            public String model() {
                return "qwen-plus";
            }
        };
        AiAnalysisService service = new AiAnalysisService(
                client,
                audits,
                Clock.fixed(Instant.parse("2026-07-12T08:00:00Z"), ZoneOffset.UTC)
        );

        AnalysisAdvice advice = service.generate(
                AnalysisMode.ONLINE_MODEL,
                7L,
                "LEARNING_REPORT",
                "JVM 正确率 20%"
        );

        assertThat(advice.source()).isEqualTo("ONLINE_MODEL");
        assertThat(audits.findRecent(10)).singleElement().satisfies(audit -> {
            assertThat(audit.userId()).isEqualTo(7L);
            assertThat(audit.purpose()).isEqualTo("LEARNING_REPORT");
            assertThat(audit.provider()).isEqualTo("OPENAI_COMPATIBLE");
            assertThat(audit.modelName()).isEqualTo("qwen-plus");
            assertThat(audit.status()).isEqualTo("SUCCESS");
            assertThat(audit.failureReason()).isNull();
            assertThat(audit.durationMs()).isNotNegative();
            assertThat(audit.createdAt()).isEqualTo(Instant.parse("2026-07-12T08:00:00Z"));
        });
    }

    @Test
    void onlineFailureWritesSanitizedAuditAndOfflineModeWritesNone() {
        InMemoryAiCallAuditRepository audits = new InMemoryAiCallAuditRepository();
        AiAnalysisService service = new AiAnalysisService(
                summary -> {
                    throw new IllegalStateException("Bearer top-secret\n" + "x".repeat(400));
                },
                audits,
                Clock.fixed(Instant.parse("2026-07-12T08:00:00Z"), ZoneOffset.UTC)
        );

        AnalysisAdvice fallback = service.generate(
                AnalysisMode.ONLINE_MODEL,
                7L,
                "LEARNING_REPORT",
                "并发编程正确率 30%"
        );
        service.generate(AnalysisMode.OFFLINE_RULES, 7L, "LEARNING_REPORT", "集合框架正确率 60%");

        assertThat(fallback.source()).isEqualTo("RULES");
        assertThat(audits.findRecent(10)).singleElement().satisfies(audit -> {
            assertThat(audit.status()).isEqualTo("FALLBACK");
            assertThat(audit.failureReason()).doesNotContain("top-secret", "\n");
            assertThat(audit.failureReason()).hasSizeLessThanOrEqualTo(240);
        });
    }

    @Test
    void metadataOrAuditPersistenceFailureCannotBlockRulesFallback() {
        OnlineModelClient client = new OnlineModelClient() {
            @Override
            public String generateAdvice(String summary) {
                throw new IllegalStateException("模型不可用");
            }

            @Override
            public String provider() {
                throw new IllegalStateException("设置仓储不可用");
            }
        };
        AiCallAuditRepository unavailableAudits = new AiCallAuditRepository() {
            @Override
            public AiCallAudit save(AiCallAudit audit) {
                throw new IllegalStateException("审计仓储不可用");
            }

            @Override
            public java.util.List<AiCallAudit> findRecent(int limit) {
                return java.util.List.of();
            }
        };
        AiAnalysisService service = new AiAnalysisService(
                client,
                unavailableAudits,
                Clock.fixed(Instant.parse("2026-07-12T08:00:00Z"), ZoneOffset.UTC)
        );

        AnalysisAdvice advice = service.generate(
                AnalysisMode.ONLINE_MODEL,
                7L,
                "LEARNING_REPORT",
                "JVM 正确率 20%"
        );

        assertThat(advice.source()).isEqualTo("RULES");
        assertThat(advice.content()).contains("已自动切换为规则分析");
    }
}
