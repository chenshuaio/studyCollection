package com.studycollection.mistake.api;

public record RecordMistakeRequest(
        Long questionId,
        String submittedAnswer,
        String sourceContext
) {
}
