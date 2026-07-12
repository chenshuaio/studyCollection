package com.studycollection.exam.app;

import com.studycollection.exam.api.ExamRuleRequest;
import com.studycollection.exam.domain.ExamRule;
import com.studycollection.exam.domain.ExamRuleStatus;
import com.studycollection.exam.domain.ExamSession;
import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ExamRuleServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-12T03:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void managesDraftPublishUpdateUnpublishAndDeleteLifecycle() {
        Fixture fixture = fixture(true);

        ExamRule created = fixture.service().create(1L, request("Java 基础模拟考试"));
        assertThat(created.status()).isEqualTo(ExamRuleStatus.DRAFT);
        assertThat(created.createdBy()).isEqualTo(1L);
        assertThat(fixture.service().listPublished()).isEmpty();

        ExamRule published = fixture.service().publish(created.id());
        assertThat(published.status()).isEqualTo(ExamRuleStatus.PUBLISHED);
        assertThat(fixture.service().listPublished()).extracting(ExamRule::id).containsExactly(created.id());

        ExamRule updated = fixture.service().update(created.id(), request("Java 基础模拟考试（新版）"));
        assertThat(updated.name()).contains("新版");
        assertThat(updated.status()).isEqualTo(ExamRuleStatus.DRAFT);
        assertThat(updated.createdAt()).isEqualTo(created.createdAt());
        assertThat(fixture.service().listPublished()).isEmpty();

        fixture.service().publish(created.id());
        ExamRule unpublished = fixture.service().unpublish(created.id());
        assertThat(unpublished.status()).isEqualTo(ExamRuleStatus.DRAFT);

        fixture.service().delete(created.id());
        assertThat(fixture.service().listAll()).isEmpty();
    }

    @Test
    void validatesAvailabilityWhenPublishingAndStartsOwnedSessionFromPublishedRule() {
        Fixture unavailable = fixture(false);
        ExamRule unavailableRule = unavailable.service().create(1L, request("不可发布规则"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> unavailable.service().publish(unavailableRule.id()))
                .withMessageContaining("题库不足");

        Fixture available = fixture(true);
        ExamRule created = available.service().create(1L, request("Java 基础模拟考试"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> available.service().start(7L, created.id()))
                .withMessageContaining("尚未发布");

        available.service().publish(created.id());
        ExamSession session = available.service().start(7L, created.id());

        assertThat(session.userId()).isEqualTo(7L);
        assertThat(session.name()).isEqualTo("Java 基础模拟考试");
        assertThat(session.durationMinutes()).isEqualTo(30);
        assertThat(session.questions()).singleElement().satisfies(question -> {
            assertThat(question.type()).isEqualTo(QuestionType.SINGLE_CHOICE);
            assertThat(question.difficulty()).isEqualTo(Difficulty.BEGINNER);
            assertThat(question.knowledgePoint()).isEqualTo("Java 基础");
        });
    }

    @Test
    void startsSessionFromGeneratedQuestionsWithoutQueryingThemAgain() {
        Question original = new Question(
                1L,
                "原始题目\nA. 0\nB. null",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "原始解析"
        );
        Question concurrentlyChanged = new Question(
                1L,
                "并发修改后的题目",
                QuestionType.SHORT_ANSWER,
                Difficulty.ADVANCED,
                "并发编程",
                "修改后的答案",
                "修改后的解析"
        );
        InMemoryQuestionRepository questions = new InMemoryQuestionRepository() {
            @Override
            public Question findById(Long id) {
                return concurrentlyChanged;
            }
        };
        questions.save(original);
        ExamSessionService sessions = new ExamSessionService(
                new InMemoryExamSessionRepository(),
                questions,
                new InMemoryPracticeStatsRepository(),
                new InMemoryLearningAttemptRepository(),
                CLOCK
        );
        ExamRuleService service = new ExamRuleService(
                new InMemoryExamRuleRepository(),
                new RuleBasedExamGenerator(questions),
                sessions,
                CLOCK
        );

        ExamRule rule = service.create(1L, request("并发一致性考试"));
        service.publish(rule.id());
        ExamSession session = service.start(7L, rule.id());

        assertThat(session.questions()).singleElement().satisfies(question -> {
            assertThat(question.title()).isEqualTo(original.title());
            assertThat(question.type()).isEqualTo(original.type());
            assertThat(question.difficulty()).isEqualTo(original.difficulty());
            assertThat(question.knowledgePoint()).isEqualTo(original.knowledgePoint());
            assertThat(question.correctAnswer()).isEqualTo(original.answer());
        });
    }

    private Fixture fixture(boolean withQuestion) {
        InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
        if (withQuestion) {
            questions.save(new Question(
                    1L,
                    "Java 中 int 默认值是多少？\nA. 0\nB. null",
                    QuestionType.SINGLE_CHOICE,
                    Difficulty.BEGINNER,
                    "Java 基础",
                    "A",
                    "成员变量的 int 默认值为 0。"
            ));
        }
        ExamSessionService sessions = new ExamSessionService(
                new InMemoryExamSessionRepository(),
                questions,
                new InMemoryPracticeStatsRepository(),
                new InMemoryLearningAttemptRepository(),
                CLOCK
        );
        ExamRuleService service = new ExamRuleService(
                new InMemoryExamRuleRepository(),
                new RuleBasedExamGenerator(questions),
                sessions,
                CLOCK
        );
        return new Fixture(service);
    }

    private ExamRuleRequest request(String name) {
        return new ExamRuleRequest(
                name,
                "覆盖 Java 基础知识",
                30,
                1,
                List.of("Java 基础"),
                Map.of(QuestionType.SINGLE_CHOICE, 1),
                Map.of(Difficulty.BEGINNER, 1)
        );
    }

    private record Fixture(ExamRuleService service) {
    }
}
