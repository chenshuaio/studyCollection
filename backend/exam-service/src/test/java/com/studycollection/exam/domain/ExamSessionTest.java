package com.studycollection.exam.domain;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExamSessionTest {
    private static final Instant STARTED_AT = Instant.parse("2026-07-11T04:00:00Z");

    @Test
    void savesLatestAnswerAndCompletesOnlyObjectiveQuestions() {
        ExamSession session = ExamSession.start(
                7L,
                "Java 综合测试",
                45,
                STARTED_AT,
                List.of(
                        question(11L, QuestionType.SINGLE_CHOICE, "B", 0),
                        question(12L, QuestionType.MULTIPLE_CHOICE, "A,C", 1),
                        question(13L, QuestionType.SHORT_ANSWER, "参考答案", 2)
                )
        );

        session = session.saveAnswer(11L, "A");
        session = session.saveAnswer(11L, "B");
        session = session.saveAnswer(12L, " c、a ");
        session = session.saveAnswer(13L, "我的理解");
        ExamSession completed = session.complete(STARTED_AT.plusSeconds(300));

        assertThat(completed.status()).isEqualTo(ExamStatus.SUBMITTED);
        assertThat(completed.submittedAt()).isEqualTo(STARTED_AT.plusSeconds(300));
        assertThat(completed.score()).isEqualTo(20);
        assertThat(completed.totalScore()).isEqualTo(20);
        assertThat(completed.answers().get(11L).submittedAnswer()).isEqualTo("B");
        assertThat(completed.answers().get(11L).autoGraded()).isTrue();
        assertThat(completed.answers().get(11L).correct()).isTrue();
        assertThat(completed.answers().get(12L).correct()).isTrue();
        assertThat(completed.answers().get(13L).autoGraded()).isFalse();
        assertThat(completed.answers().get(13L).correct()).isNull();
        assertThat(completed.answers().get(13L).score()).isZero();
    }

    @Test
    void createsBlankAnswersForUnansweredQuestionsWhenTimeExpires() {
        ExamSession session = ExamSession.start(
                7L,
                "限时测试",
                1,
                STARTED_AT,
                List.of(question(21L, QuestionType.TRUE_FALSE, "true", 0))
        );

        ExamSession completed = session.complete(STARTED_AT.plusSeconds(60));

        assertThat(completed.answers().get(21L).submittedAnswer()).isEmpty();
        assertThat(completed.answers().get(21L).autoGraded()).isTrue();
        assertThat(completed.answers().get(21L).correct()).isFalse();
        assertThat(completed.score()).isZero();
        assertThat(completed.totalScore()).isEqualTo(10);
    }

    private ExamQuestionSnapshot question(Long id, QuestionType type, String correctAnswer, int sortOrder) {
        return new ExamQuestionSnapshot(
                id,
                "题目 " + id,
                type,
                Difficulty.INTERMEDIATE,
                "Java 基础",
                correctAnswer,
                "题目解析",
                sortOrder
        );
    }
}
