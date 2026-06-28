package com.studycollection.report.app;

public record LearningReportResponse(
        String weakestKnowledgePoint,
        String recommendation,
        String adviceSource,
        String adviceContent
) {
}
