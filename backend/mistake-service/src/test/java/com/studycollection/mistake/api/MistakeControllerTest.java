package com.studycollection.mistake.api;

import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.common.security.Role;
import com.studycollection.mistake.app.InMemoryMistakeRepository;
import com.studycollection.mistake.app.MistakeService;
import com.studycollection.mistake.domain.MistakeRecord;
import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MistakeControllerTest {
    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "alice", Role.USER);

    @Test
    void recordsTrustedQuestionSnapshotAndAccumulatesRepeatedMistakes() {
        InMemoryMistakeRepository mistakes = new InMemoryMistakeRepository();
        InMemoryQuestionRepository questions = questions();
        MistakeController firstController = controller(
                mistakes,
                questions,
                Instant.parse("2026-07-10T08:00:00Z")
        );

        MistakeRecord first = firstController.record(USER, new RecordMistakeRequest(
                1L,
                "A",
                "PRACTICE"
        )).data();
        MistakeController secondController = controller(
                mistakes,
                questions,
                Instant.parse("2026-07-12T09:30:00Z")
        );
        MistakeRecord repeated = secondController.record(USER, new RecordMistakeRequest(
                1L,
                "C",
                "EXAM"
        )).data();

        assertThat(first.questionTitle()).isEqualTo("HashMap 默认负载因子是多少？");
        assertThat(first.questionType()).isEqualTo(QuestionType.SINGLE_CHOICE);
        assertThat(first.wrongCount()).isEqualTo(1);
        assertThat(repeated.wrongCount()).isEqualTo(2);
        assertThat(repeated.firstWrongAt()).isEqualTo(Instant.parse("2026-07-10T08:00:00Z"));
        assertThat(repeated.lastWrongAt()).isEqualTo(Instant.parse("2026-07-12T09:30:00Z"));
        assertThat(repeated.lastSubmittedAnswer()).isEqualTo("C");
        assertThat(repeated.sourceContext()).isEqualTo("EXAM");
        assertThat(repeated.status()).isEqualTo("PENDING");
    }

    @Test
    void filtersMistakesByKnowledgePointTypeDateAndStatus() {
        InMemoryMistakeRepository mistakes = new InMemoryMistakeRepository();
        InMemoryQuestionRepository questions = questions();
        controller(mistakes, questions, Instant.parse("2026-07-09T08:00:00Z"))
                .record(USER, new RecordMistakeRequest(1L, "A", "PRACTICE"));
        MistakeController controller = controller(
                mistakes,
                questions,
                Instant.parse("2026-07-12T08:00:00Z")
        );
        controller.record(USER, new RecordMistakeRequest(2L, "错误答案", "EXAM"));

        List<MistakeRecord> matches = controller.list(
                USER,
                "JVM",
                QuestionType.FILL_BLANK,
                "PENDING",
                LocalDate.parse("2026-07-11"),
                LocalDate.parse("2026-07-12")
        ).data();

        assertThat(matches).singleElement().satisfies(record -> {
            assertThat(record.questionId()).isEqualTo(2L);
            assertThat(record.knowledgePoint()).isEqualTo("JVM");
        });
    }

    @Test
    void changingMasteryStatusDoesNotIncreaseWrongCount() {
        InMemoryMistakeRepository mistakes = new InMemoryMistakeRepository();
        MistakeController controller = controller(
                mistakes,
                questions(),
                Instant.parse("2026-07-12T08:00:00Z")
        );
        controller.record(USER, new RecordMistakeRequest(1L, "A", "PRACTICE"));

        MistakeRecord updated = controller.updateStatus(USER, new UpdateMistakeStatusRequest(
                1L,
                "MASTERED"
        )).data();

        assertThat(updated.status()).isEqualTo("MASTERED");
        assertThat(updated.wrongCount()).isEqualTo(1);
        assertThat(updated.firstWrongAt()).isEqualTo(updated.lastWrongAt());
    }

    private InMemoryQuestionRepository questions() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        repository.save(new Question(
                1L,
                "HashMap 默认负载因子是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "B",
                "默认负载因子是 0.75。"
        ));
        repository.save(new Question(
                2L,
                "JVM 栈帧主要保存什么？",
                QuestionType.FILL_BLANK,
                Difficulty.INTERMEDIATE,
                "JVM",
                "局部变量表",
                "每次方法调用都会创建栈帧。"
        ));
        return repository;
    }

    private MistakeController controller(
            InMemoryMistakeRepository mistakes,
            InMemoryQuestionRepository questions,
            Instant now
    ) {
        return new MistakeController(new MistakeService(
                mistakes,
                questions,
                Clock.fixed(now, ZoneOffset.UTC)
        ));
    }
}
