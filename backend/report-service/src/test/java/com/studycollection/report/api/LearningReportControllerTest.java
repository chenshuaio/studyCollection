package com.studycollection.report.api;

import com.studycollection.ai.app.AiAnalysisService;
import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.common.security.Role;
import com.studycollection.exam.app.InMemoryLearningAttemptRepository;
import com.studycollection.exam.app.LearningActivityType;
import com.studycollection.exam.app.LearningAttempt;
import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.app.InMemoryQuestionFeedbackRepository;
import com.studycollection.question.app.QuestionFeedbackService;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.FeedbackType;
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
        questions.save(new Question(
                93L,
                USER.userId(),
                "我的 JVM 个人强化题",
                QuestionType.FILL_BLANK,
                Difficulty.BEGINNER,
                "JVM",
                "字节码",
                "个人强化题解析"
        ));
        questions.save(new Question(
                94L,
                OTHER_USER.userId(),
                "他人的 JVM 个人强化题",
                QuestionType.FILL_BLANK,
                Difficulty.BEGINNER,
                "JVM",
                "类加载器",
                "他人强化题解析"
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
        assertThat(report.strengtheningQuestions()).hasSize(3);
        assertThat(report.strengtheningQuestions()).extracting(question -> question.id())
                .containsExactly(91L, 92L, 93L)
                .doesNotContain(94L);
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

    @Test
    void excludesOrRecalculatesAttemptsAffectedByAcceptedAnswerRevision() {
        InMemoryLearningAttemptRepository attemptRepository = new InMemoryLearningAttemptRepository();
        InMemoryLearningReportRepository reportRepository = new InMemoryLearningReportRepository();
        InMemoryQuestionRepository questionRepository = new InMemoryQuestionRepository();
        Question revisedQuestion = questionRepository.save(new Question(
                101L,
                "Java 中 int 成员变量默认值是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "旧解析"
        ));
        questionRepository.save(new Question(
                102L,
                "String 是否不可变？",
                QuestionType.TRUE_FALSE,
                Difficulty.BEGINNER,
                "Java 基础",
                "true",
                "String 是不可变类。"
        ));
        InMemoryQuestionFeedbackRepository feedbackRepository = new InMemoryQuestionFeedbackRepository();
        QuestionFeedbackService feedbackService = new QuestionFeedbackService(
                feedbackRepository,
                questionRepository,
                Clock.fixed(Instant.parse("2026-07-12T08:00:00Z"), ZoneOffset.UTC)
        );
        var feedback = feedbackService.submit(
                7L,
                revisedQuestion.id(),
                FeedbackType.ANSWER_ERROR,
                "标准答案应为 B"
        );
        feedbackService.accept(
                feedback.id(),
                1L,
                "答案从 A 修改为 B",
                "已核验",
                "B",
                "正确答案是 B。"
        );
        attemptRepository.saveAll(List.of(
                attemptWithAnswer(7L, 101L, "Java 基础", QuestionType.SINGLE_CHOICE, "B", false),
                attemptWithAnswer(7L, 102L, "Java 基础", QuestionType.TRUE_FALSE, "true", true)
        ));
        LearningReportService service = new LearningReportService(
                attemptRepository,
                reportRepository,
                new WeakPointAnalyzer(),
                new AiAnalysisService(summary -> "在线建议"),
                questionRepository,
                feedbackRepository,
                Clock.fixed(Instant.parse("2026-07-12T09:00:00Z"), ZoneOffset.UTC)
        );
        LearningReportController controller = new LearningReportController(service);

        LearningReportResponse excluded = controller.generate(
                USER,
                new LearningReportRequest("OFFLINE_RULES", "EXCLUDE_REVISED")
        ).data();
        LearningReportResponse recalculated = controller.generate(
                USER,
                new LearningReportRequest("OFFLINE_RULES", "RECALCULATE_REVISED")
        ).data();

        assertThat(excluded.revisionPolicy()).isEqualTo("EXCLUDE_REVISED");
        assertThat(excluded.revisedAttemptCount()).isEqualTo(1);
        assertThat(excluded.gradedQuestionCount()).isEqualTo(1);
        assertThat(excluded.correctQuestionCount()).isEqualTo(1);
        assertThat(recalculated.revisionPolicy()).isEqualTo("RECALCULATE_REVISED");
        assertThat(recalculated.revisedAttemptCount()).isEqualTo(1);
        assertThat(recalculated.gradedQuestionCount()).isEqualTo(2);
        assertThat(recalculated.correctQuestionCount()).isEqualTo(2);
        assertThat(recalculated.accuracy()).isEqualTo(1.0);
        assertThat(attemptRepository.findByUserId(7L).get(0).correct()).isFalse();
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

    private LearningAttempt attemptWithAnswer(
            Long userId,
            Long questionId,
            String knowledgePoint,
            QuestionType type,
            String submittedAnswer,
            boolean correct
    ) {
        return new LearningAttempt(
                null,
                userId,
                LearningActivityType.PRACTICE,
                "revision-practice-" + questionId,
                questionId,
                "第 " + questionId + " 题",
                type,
                Difficulty.BEGINNER,
                knowledgePoint,
                submittedAnswer,
                true,
                correct,
                correct ? 10 : 0,
                Instant.parse("2026-07-12T07:00:00Z")
        );
    }
}
