package com.studycollection.importer.parser;

public record ParsedQuestion(
        String title,
        String answer,
        String knowledgePoint,
        String difficulty
) {
}
