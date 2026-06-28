package com.studycollection.ai.app;

import org.junit.jupiter.api.Test;

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
}
