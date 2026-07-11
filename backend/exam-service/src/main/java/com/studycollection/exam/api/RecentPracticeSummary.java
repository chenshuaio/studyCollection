package com.studycollection.exam.api;

import java.time.Instant;
import java.util.List;

public record RecentPracticeSummary(
        String referenceId,
        Instant attemptedAt,
        int answeredQuestionCount,
        int gradedQuestionCount,
        int correctQuestionCount,
        double accuracy,
        List<String> knowledgePoints
) {
    public RecentPracticeSummary {
        knowledgePoints = List.copyOf(knowledgePoints);
    }
}
