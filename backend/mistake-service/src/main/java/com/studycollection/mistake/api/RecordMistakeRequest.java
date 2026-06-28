package com.studycollection.mistake.api;

public record RecordMistakeRequest(
        Long userId,
        Long questionId,
        String questionTitle,
        String knowledgePoint,
        String status
) {
}
