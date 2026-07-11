package com.studycollection.exam.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLearningAttemptRepositoryTest {
    @Test
    void keepsAttemptKeysIdempotentAndIsolatesUsers() {
        InMemoryLearningAttemptRepository repository = new InMemoryLearningAttemptRepository();
        LearningAttempt first = attempt(7L, "exam-1", 11L);

        repository.saveAll(List.of(first));
        repository.saveAll(List.of(first));
        repository.saveAll(List.of(attempt(8L, "exam-1", 11L)));

        assertThat(repository.findByUserId(7L)).singleElement().satisfies(saved -> {
            assertThat(saved.id()).isPositive();
            assertThat(saved.questionTitle()).isEqualTo("Java 关键字题");
        });
        assertThat(repository.findByUserId(8L)).hasSize(1);
    }

    private LearningAttempt attempt(Long userId, String referenceId, Long questionId) {
        return new LearningAttempt(
                null,
                userId,
                LearningActivityType.EXAM,
                referenceId,
                questionId,
                "Java 关键字题",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                true,
                true,
                10,
                Instant.parse("2026-07-11T08:00:00Z")
        );
    }
}
