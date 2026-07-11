package com.studycollection.exam.app;

import com.studycollection.exam.api.PracticeStats;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!local-mysql")
public class InMemoryPracticeStatsRepository implements PracticeStatsRepository {
    private final Map<Long, PracticeStats> statsByUser = new ConcurrentHashMap<>();

    @Override
    public PracticeStats add(Long userId, int answeredQuestionCount, int correctQuestionCount) {
        return statsByUser.merge(
                userId,
                new PracticeStats(userId, answeredQuestionCount, correctQuestionCount),
                (existing, current) -> new PracticeStats(
                        userId,
                        existing.answeredQuestionCount() + current.answeredQuestionCount(),
                        existing.correctQuestionCount() + current.correctQuestionCount()
                )
        );
    }

    @Override
    public PracticeStats findByUserId(Long userId) {
        return statsByUser.getOrDefault(userId, new PracticeStats(userId, 0, 0));
    }
}
