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
}
