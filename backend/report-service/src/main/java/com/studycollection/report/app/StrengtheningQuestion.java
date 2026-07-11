package com.studycollection.report.app;

public record StrengtheningQuestion(
        Long id,
        String title,
        String type,
        String difficulty,
        String knowledgePoint
) {
}
