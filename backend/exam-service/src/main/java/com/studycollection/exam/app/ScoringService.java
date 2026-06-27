package com.studycollection.exam.app;

import java.util.Map;

public class ScoringService {
    public int score(Map<Long, String> correctAnswers, Map<Long, String> submittedAnswers, int pointsPerQuestion) {
        return correctAnswers.entrySet().stream()
                .mapToInt(entry -> entry.getValue().equals(submittedAnswers.get(entry.getKey())) ? pointsPerQuestion : 0)
                .sum();
    }
}
