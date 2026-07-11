package com.studycollection.exam.app;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("!local-mysql")
public class InMemoryLearningAttemptRepository implements LearningAttemptRepository {
    private final AtomicLong sequence = new AtomicLong(1);
    private final Map<String, LearningAttempt> attempts = new LinkedHashMap<>();

    @Override
    public synchronized void saveAll(List<LearningAttempt> candidates) {
        candidates.forEach(candidate -> attempts.computeIfAbsent(
                uniqueKey(candidate),
                ignored -> candidate.withId(sequence.getAndIncrement())
        ));
    }

    @Override
    public synchronized List<LearningAttempt> findByUserId(Long userId) {
        return attempts.values().stream()
                .filter(attempt -> attempt.userId().equals(userId))
                .sorted(Comparator.comparing(LearningAttempt::attemptedAt).thenComparing(LearningAttempt::id))
                .toList();
    }

    private String uniqueKey(LearningAttempt attempt) {
        return attempt.userId() + "|" + attempt.activityType() + "|" + attempt.referenceId() + "|" + attempt.questionId();
    }
}
