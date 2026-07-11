package com.studycollection.report.app;

import java.time.LocalDate;

public record TrendPoint(
        LocalDate date,
        int gradedQuestionCount,
        int correctQuestionCount,
        double accuracy
) {
}
