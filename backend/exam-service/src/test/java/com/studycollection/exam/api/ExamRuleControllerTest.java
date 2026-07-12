package com.studycollection.exam.api;

import com.studycollection.common.security.AdminOnly;
import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.common.security.Role;
import com.studycollection.exam.app.ExamRuleService;
import com.studycollection.exam.app.ExamSessionService;
import com.studycollection.exam.app.InMemoryExamRuleRepository;
import com.studycollection.exam.app.InMemoryExamSessionRepository;
import com.studycollection.exam.app.InMemoryLearningAttemptRepository;
import com.studycollection.exam.app.InMemoryPracticeStatsRepository;
import com.studycollection.exam.app.RuleBasedExamGenerator;
import com.studycollection.exam.domain.ExamRuleStatus;
import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExamRuleControllerTest {
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1L, "admin", Role.ADMIN);
    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "alice", Role.USER);

    @Test
    void exposesAdminLifecycleAndStartsPublishedSimulationForUser() throws Exception {
        ExamRuleController controller = controller();
        ExamRuleRequest request = new ExamRuleRequest(
                "Java 入门模拟考试",
                "检验基础语法",
                20,
                1,
                List.of("Java 基础"),
                Map.of(QuestionType.SINGLE_CHOICE, 1),
                Map.of(Difficulty.BEGINNER, 1)
        );

        var created = controller.create(ADMIN, request).data();
        assertThat(created.status()).isEqualTo(ExamRuleStatus.DRAFT);
        assertThat(controller.listPublished().data()).isEmpty();
        assertThat(controller.listAll().data()).singleElement().satisfies(rule ->
                assertThat(rule.id()).isEqualTo(created.id()));

        controller.publish(created.id());
        assertThat(controller.listPublished().data()).singleElement().satisfies(rule ->
                assertThat(rule.status()).isEqualTo(ExamRuleStatus.PUBLISHED));

        ExamSessionResponse started = controller.start(USER, created.id()).data();
        assertThat(started.name()).isEqualTo("Java 入门模拟考试");
        assertThat(started.questions()).hasSize(1);
        assertThat(started.questions().get(0).correctAnswer()).isEmpty();

        Method createMethod = ExamRuleController.class.getMethod(
                "create",
                AuthenticatedUser.class,
                ExamRuleRequest.class
        );
        Method listAllMethod = ExamRuleController.class.getMethod("listAll");
        assertThat(createMethod.isAnnotationPresent(AdminOnly.class)).isTrue();
        assertThat(listAllMethod.isAnnotationPresent(AdminOnly.class)).isTrue();
    }

    private ExamRuleController controller() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-12T03:00:00Z"), ZoneOffset.UTC);
        InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
        questions.save(new Question(
                1L,
                "Java 中 int 默认值是多少？\nA. 0\nB. null",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "成员变量的 int 默认值为 0。"
        ));
        ExamSessionService sessions = new ExamSessionService(
                new InMemoryExamSessionRepository(),
                questions,
                new InMemoryPracticeStatsRepository(),
                new InMemoryLearningAttemptRepository(),
                clock
        );
        ExamRuleService service = new ExamRuleService(
                new InMemoryExamRuleRepository(),
                new RuleBasedExamGenerator(questions),
                sessions,
                clock
        );
        return new ExamRuleController(service, clock);
    }
}
