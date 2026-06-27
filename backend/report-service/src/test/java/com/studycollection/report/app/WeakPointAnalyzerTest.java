package com.studycollection.report.app;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeakPointAnalyzerTest {
    @Test
    void identifiesLowestAccuracyKnowledgePoint() {
        WeakPointAnalyzer analyzer = new WeakPointAnalyzer();

        LearningReport report = analyzer.analyze(List.of(
                new WeakPointAnalyzer.Result("集合框架", true),
                new WeakPointAnalyzer.Result("集合框架", false),
                new WeakPointAnalyzer.Result("JVM", false),
                new WeakPointAnalyzer.Result("JVM", false)
        ));

        assertThat(report.weakestKnowledgePoint()).isEqualTo("JVM");
        assertThat(report.recommendation()).contains("JVM");
    }
}
