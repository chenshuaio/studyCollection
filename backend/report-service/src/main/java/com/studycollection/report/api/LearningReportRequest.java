package com.studycollection.report.api;

public record LearningReportRequest(String mode, String revisedQuestionPolicy) {
    public LearningReportRequest(String mode) {
        this(mode, "EXCLUDE_REVISED");
    }
}
