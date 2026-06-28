package com.studycollection.mistake.api;

public record UpdateMistakeStatusRequest(
        Long userId,
        Long questionId,
        String status
) {
}
