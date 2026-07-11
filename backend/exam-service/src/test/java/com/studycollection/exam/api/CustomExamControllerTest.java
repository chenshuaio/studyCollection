package com.studycollection.exam.api;

import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.common.security.Role;
import com.studycollection.exam.app.ExamSessionService;
import com.studycollection.exam.app.InMemoryExamSessionRepository;
import com.studycollection.exam.app.InMemoryPracticeStatsRepository;
import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomExamControllerTest {
    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "alice", Role.USER);

    @Test
    void supportsAuthenticatedCreateSaveSubmitAndHistoryWithoutLeakingAnswersEarly() {
        CustomExamController controller = controller();

        ExamSessionResponse created = controller.create(USER, new CustomExamRequest(
                "集合专项测试",
                45,
                List.of(1L)
        )).data();

        assertThat(created.id()).isPositive();
        assertThat(created.questions()).singleElement().satisfies(question -> {
            assertThat(question.correctAnswer()).isEmpty();
            assertThat(question.analysis()).isEmpty();
            assertThat(question.submittedAnswer()).isEmpty();
        });

        ExamSessionResponse saved = controller.saveAnswer(
                USER,
                created.id(),
                1L,
                new SaveExamAnswerRequest("A")
        ).data();
        assertThat(saved.questions().get(0).submittedAnswer()).isEqualTo("A");
        assertThat(saved.questions().get(0).correctAnswer()).isEmpty();

        ExamSessionResponse submitted = controller.submit(USER, created.id()).data();
        assertThat(submitted.status()).isEqualTo("SUBMITTED");
        assertThat(submitted.questions().get(0).correctAnswer()).isEqualTo("A");
        assertThat(submitted.questions().get(0).analysis()).contains("HashMap");
        assertThat(submitted.questions().get(0).correct()).isTrue();
        assertThat(controller.list(USER).data()).singleElement().satisfies(summary -> {
            assertThat(summary.id()).isEqualTo(created.id());
            assertThat(summary.answeredCount()).isEqualTo(1);
        });
    }

    private CustomExamController controller() {
        InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
        questions.save(new Question(
                1L,
                "HashMap 是否允许 null 键？\nA. 允许\nB. 不允许",
                QuestionType.SINGLE_CHOICE,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "A",
                "HashMap 允许一个 null 键。"
        ));
        ExamSessionService service = new ExamSessionService(
                new InMemoryExamSessionRepository(),
                questions,
                new InMemoryPracticeStatsRepository(),
                Clock.fixed(Instant.parse("2026-07-11T06:00:00Z"), ZoneOffset.UTC)
        );
        return new CustomExamController(service, Clock.fixed(
                Instant.parse("2026-07-11T06:00:00Z"),
                ZoneOffset.UTC
        ));
    }
}
