package com.studycollection.report.api;

import com.studycollection.ai.app.AiAnalysisService;
import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.common.security.Role;
import com.studycollection.exam.app.InMemoryLearningAttemptRepository;
import com.studycollection.exam.app.LearningActivityType;
import com.studycollection.exam.app.LearningAttempt;
import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import com.studycollection.report.app.InMemoryLearningReportRepository;
import com.studycollection.report.app.LearningReportResponse;
import com.studycollection.report.app.LearningReportService;
import com.studycollection.report.app.WeakPointAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LearningReportControllerTest {
    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "alice", Role.USER);
    private static final AuthenticatedUser OTHER_USER = new AuthenticatedUser(8L, "bob", Role.USER);

    private InMemoryLearningAttemptRepository attempts;
    private LearningReportController controller;

    @BeforeEach
    void setUp() {
        attempts = new InMemoryLearningAttemptRepository();
        InMemoryLearningReportRepository reports = new InMemoryLearningReportRepository();
        InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
        questions.save(new Question(
                91L,
                "JVM 堆中主要保存什么？",
                QuestionType.FILL_BLANK,
                Difficulty.INTERMEDIATE,
                "JVM",
                "对象实例和数组",
                "强化题解析不应在报告响应中泄露。"
        ));
        questions.save(new Question(
                92L,
                "说明 JVM 垃圾回收的目标。",
                QuestionType.SHORT_ANSWER,
                Difficulty.ADVANCED,
                "JVM",
                "回收不可达对象占用的内存。",
                "强化题解析"
        ));
        AiAnalysisService aiAnalysisService = new AiAnalysisService(summary -> {
            throw new IllegalStateException("测试中的在线模型不可用");
        });
        LearningReportService service = new LearningReportService(
                attempts,
                reports,
                new WeakPointAnalyzer(),
                aiAnalysisService,
                questions,
                Clock.fixed(Instant.parse("2026-07-11T09:00:00Z"), ZoneOffset.UTC)
        );
        controller = new LearningReportController(service);
    }

    @Test
    void generatesTrustedReportWithBreakdownsTrendRecommendationsAndHistory() {
        attempts.saveAll(List.of(
                attempt(7L, 1L, "JVM", QuestionType.FILL_BLANK, false, Instant.parse("2026-07-09T08:00:00Z")),
                subjectiveAttempt(7L, 2L, "JVM", Instant.parse("2026-07-10T08:00:00Z")),
                attempt(7L, 3L, "集合框架", QuestionType.SINGLE_CHOICE, true, Instant.parse("2026-07-10T09:00:00Z")),
                attempt(7L, 4L, "集合框架", QuestionType.SINGLE_CHOICE, false, Instant.parse("2026-07-10T10:00:00Z")),
                unansweredObjectiveAttempt(7L, 6L, "JVM", Instant.parse("2026-07-10T10:30:00Z")),
                attempt(8L, 5L, "并发编程", QuestionType.TRUE_FALSE, false, Instant.parse("2026-07-10T11:00:00Z"))
        ));

        LearningReportResponse report = controller.generate(
                USER,
                new LearningReportRequest("OFFLINE_RULES")
        ).data();

        assertThat(report.id()).isPositive();
        assertThat(report.createdAt()).isEqualTo(Instant.parse("2026-07-11T09:00:00Z"));
        assertThat(report.weakestKnowledgePoint()).isEqualTo("JVM");
        assertThat(report.answeredQuestionCount()).isEqualTo(4);
        assertThat(report.gradedQuestionCount()).isEqualTo(4);
        assertThat(report.correctQuestionCount()).isEqualTo(1);
        assertThat(report.accuracy()).isEqualTo(0.25);
        assertThat(report.knowledgePointPerformance()).hasSize(2);
        assertThat(report.knowledgePointPerformance()).anySatisfy(item -> {
            assertThat(item.label()).isEqualTo("JVM");
            assertThat(item.answeredQuestionCount()).isEqualTo(2);
            assertThat(item.gradedQuestionCount()).isEqualTo(2);
            assertThat(item.correctQuestionCount()).isZero();
        });
        assertThat(report.questionTypePerformance()).extracting(item -> item.label())
                .contains("FILL_BLANK", "SHORT_ANSWER", "SINGLE_CHOICE");
        assertThat(report.recentTrend()).extracting(point -> point.date().toString())
                .containsExactly("2026-07-09", "2026-07-10");
        assertThat(report.strengtheningQuestions()).hasSize(2);
        assertThat(report.strengtheningQuestions()).allSatisfy(question -> {
            assertThat(question.knowledgePoint()).isEqualTo("JVM");
            assertThat(question.title()).isNotBlank();
        });
        assertThat(report.adviceSource()).isEqualTo("RULES");

        assertThat(controller.history(USER).data()).singleElement().isEqualTo(report);
        assertThat(controller.history(OTHER_USER).data()).isEmpty();
    }

    @Test
    void onlineModeFallsBackToRulesAndPersistsEachSnapshotNewestFirst() {
        attempts.saveAll(List.of(attempt(
                7L,
                11L,
                "JVM",
                QuestionType.FILL_BLANK,
                false,
                Instant.parse("2026-07-10T08:00:00Z")
        )));

        LearningReportResponse first = controller.generate(USER, new LearningReportRequest("ONLINE_MODEL")).data();
        LearningReportResponse second = controller.generate(USER, new LearningReportRequest("OFFLINE_RULES")).data();

        assertThat(first.adviceSource()).isEqualTo("RULES");
        assertThat(first.adviceContent()).contains("在线模型暂不可用");
        assertThat(controller.history(USER).data()).extracting(LearningReportResponse::id)
                .containsExactly(second.id(), first.id());
    }

    @Test
    void rejectsGenerationWithoutTrustedAttempts() {
        assertThatThrownBy(() -> controller.generate(USER, new LearningReportRequest("OFFLINE_RULES")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("暂无可用于分析的真实作答记录");
    }

    private LearningAttempt attempt(
            Long userId,
            Long questionId,
            String knowledgePoint,
            QuestionType type,
            boolean correct,
            Instant attemptedAt
    ) {
        return new LearningAttempt(
                null,
                userId,
                LearningActivityType.PRACTICE,
                "practice-" + questionId,
                questionId,
                "第 " + questionId + " 题",
                type,
                Difficulty.INTERMEDIATE,
                knowledgePoint,
                "A",
                true,
                correct,
                correct ? 10 : 0,
                attemptedAt
        );
    }

    private LearningAttempt subjectiveAttempt(Long userId, Long questionId, String knowledgePoint, Instant attemptedAt) {
        return new LearningAttempt(
                null,
                userId,
                LearningActivityType.PRACTICE,
                "practice-" + questionId,
                questionId,
                "第 " + questionId + " 题",
                QuestionType.SHORT_ANSWER,
                Difficulty.ADVANCED,
                knowledgePoint,
                "我的理解",
                false,
                null,
                0,
                attemptedAt
        );
    }

    private LearningAttempt unansweredObjectiveAttempt(
            Long userId,
            Long questionId,
            String knowledgePoint,
            Instant attemptedAt
    ) {
        return new LearningAttempt(
                null,
                userId,
                LearningActivityType.EXAM,
                "exam-" + questionId,
                questionId,
                "第 " + questionId + " 题",
                QuestionType.SINGLE_CHOICE,
                Difficulty.INTERMEDIATE,
                knowledgePoint,
                "",
                true,
                false,
                0,
                attemptedAt
        );
    }
}
