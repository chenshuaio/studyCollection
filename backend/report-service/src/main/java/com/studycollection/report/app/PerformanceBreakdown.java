package com.studycollection.report.app;

public record PerformanceBreakdown(
        String label,
        int answeredQuestionCount,
        int gradedQuestionCount,
        int correctQuestionCount,
        double accuracy
) {
}
