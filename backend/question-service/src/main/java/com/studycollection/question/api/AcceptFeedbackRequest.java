package com.studycollection.question.api;

public record AcceptFeedbackRequest(
        String changeSummary,
        String reviewNote,
        String correctedAnswer,
        String correctedAnalysis
) {
}
