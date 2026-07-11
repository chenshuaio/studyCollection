package com.studycollection.exam.app;

import com.studycollection.exam.domain.ExamSession;
import com.studycollection.exam.domain.ExamQuestionSnapshot;
import com.studycollection.exam.domain.ExamStatus;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryExamSessionRepositoryTest {
    @Test
    void persistsAnswersCompletionAndNewestFirstHistory() {
        InMemoryExamSessionRepository repository = new InMemoryExamSessionRepository();
        ExamSession first = repository.create(session(7L, "第一套", Instant.parse("2026-07-11T01:00:00Z")));
        ExamSession second = repository.create(session(7L, "第二套", Instant.parse("2026-07-11T02:00:00Z")));

        repository.save(first.saveAnswer(11L, "B"));
        ExamSession reloaded = repository.findById(first.id());
        repository.save(reloaded.complete(Instant.parse("2026-07-11T01:05:00Z")));

        ExamSession completed = repository.findById(first.id());
        assertThat(completed.status()).isEqualTo(ExamStatus.SUBMITTED);
        assertThat(completed.answers().get(11L).submittedAnswer()).isEqualTo("B");
        assertThat(completed.score()).isEqualTo(10);
        assertThat(repository.findByUserId(7L))
                .extracting(ExamSession::id)
                .containsExactly(second.id(), first.id());
        assertThat(repository.findByUserId(8L)).isEmpty();
    }

    private ExamSession session(Long userId, String name, Instant startedAt) {
        return ExamSession.start(userId, name, 30, startedAt, List.of(new ExamQuestionSnapshot(
                11L,
                "HashMap 默认负载因子是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "B",
                "默认负载因子是 0.75。",
                0
        )));
    }
}
