package com.studycollection.importer.parser;

public record ParsedQuestion(
        String title,
        String type,
        String difficulty,
        String knowledgePoint,
        String answer,
        String analysis
) {
}
