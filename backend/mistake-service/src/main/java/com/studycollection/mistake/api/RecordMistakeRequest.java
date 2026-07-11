package com.studycollection.mistake.api;

public record RecordMistakeRequest(
        Long questionId,
        String questionTitle,
        String knowledgePoint,
        String status
) {
}
