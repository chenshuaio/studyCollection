package com.studycollection.question.domain;

public record KnowledgePoint(
        Long id,
        String name,
        String description,
        boolean enabled
) {
}
