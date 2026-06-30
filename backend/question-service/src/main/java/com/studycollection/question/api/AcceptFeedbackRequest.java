package com.studycollection.question.api;

public record AcceptFeedbackRequest(
        Long adminUserId,
        String changeSummary,
        String reviewNote,
        String correctedAnswer,
        String correctedAnalysis
) {
}
