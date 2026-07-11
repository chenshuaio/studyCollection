package com.studycollection.exam.app;

import com.studycollection.exam.api.PracticeStats;

public interface PracticeStatsRepository {
    PracticeStats add(Long userId, int answeredQuestionCount, int correctQuestionCount);

    PracticeStats findByUserId(Long userId);
}
