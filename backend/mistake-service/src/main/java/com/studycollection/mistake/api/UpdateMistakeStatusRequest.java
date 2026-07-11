package com.studycollection.mistake.api;

public record UpdateMistakeStatusRequest(
        Long questionId,
        String status
) {
}
