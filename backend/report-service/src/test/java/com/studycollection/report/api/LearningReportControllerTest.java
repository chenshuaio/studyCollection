package com.studycollection.report.api;

import com.studycollection.report.app.LearningReportResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LearningReportControllerTest {
    @Test
    void generatesRuleReportWithWeakPointAndAdvice() {
        LearningReportController controller = new LearningReportController();

        LearningReportResponse report = controller.generate(new LearningReportRequest(
                "OFFLINE_RULES",
                List.of(
                        new ReportResultRequest("集合框架", true),
                        new ReportResultRequest("JVM", false),
                        new ReportResultRequest("JVM", false)
                )
        )).data();

        assertThat(report.weakestKnowledgePoint()).isEqualTo("JVM");
        assertThat(report.recommendation()).contains("JVM");
        assertThat(report.adviceSource()).isEqualTo("RULES");
        assertThat(report.adviceContent()).contains("JVM");
    }
}
