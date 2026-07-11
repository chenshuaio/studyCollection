package com.studycollection.exam.api;

import java.util.List;

public record GeneratedPractice(
        int requestedCount,
        int actualCount,
        List<GeneratedPracticeQuestion> questions
) {
}
