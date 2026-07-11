package com.studycollection.exam.api;

import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.common.security.Role;
import com.studycollection.exam.app.InMemoryPracticeStatsRepository;
import com.studycollection.exam.app.InMemoryLearningAttemptRepository;
import com.studycollection.exam.app.PracticeGenerator;
import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PracticeControllerTest {
    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "alice", Role.USER);
    private static final AuthenticatedUser OTHER_USER = new AuthenticatedUser(8L, "bob", Role.USER);

    @Test
    void scoresSubmittedPracticeAndReturnsExplanations() {
        PracticeController controller = controllerWithSampleQuestions();

        PracticeResult result = controller.submit(USER, new PracticeSubmitRequest(List.of(
                new PracticeAnswer(1L, "A"),
                new PracticeAnswer(2L, "true"),
                new PracticeAnswer(3L, "B")
        ))).data();

        assertThat(result.score()).isEqualTo(20);
        assertThat(result.totalScore()).isEqualTo(30);
        assertThat(result.items()).hasSize(3);
        assertThat(result.items().get(0).correct()).isTrue();
        assertThat(result.items().get(0).analysis()).contains("HashMap");
        assertThat(result.items().get(1).correct()).isTrue();
        assertThat(result.items().get(1).analysis()).contains("局部变量");
        assertThat(result.items().get(2).correct()).isFalse();
        assertThat(result.items().get(2).analysis()).contains("ArrayList");
    }

    @Test
    void tracksSubmittedQuestionCountForAuthenticatedUser() {
        PracticeController controller = controllerWithSampleQuestions();

        controller.submit(USER, new PracticeSubmitRequest(List.of(
                new PracticeAnswer(1L, "A"),
                new PracticeAnswer(2L, "true")
        )));
        controller.submit(USER, new PracticeSubmitRequest(List.of(
                new PracticeAnswer(3L, "B")
        )));

        PracticeStats stats = controller.stats(USER).data();

        assertThat(stats.userId()).isEqualTo(USER.userId());
        assertThat(stats.answeredQuestionCount()).isEqualTo(3);
        assertThat(stats.gradedQuestionCount()).isEqualTo(3);
        assertThat(stats.correctQuestionCount()).isEqualTo(2);
    }

    @Test
    void scoresUsingQuestionBankAnswerAndAnalysis() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        repository.save(new Question(
                99L,
                "HashMap 默认负载因子是多少？",
                QuestionType.FILL_BLANK,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "0.75",
                "HashMap 默认负载因子是 0.75。"
        ));
        PracticeController controller = new PracticeController(repository, new InMemoryPracticeStatsRepository());

        PracticeResult result = controller.submit(USER, new PracticeSubmitRequest(List.of(
                new PracticeAnswer(99L, "0.75")
        ))).data();

        assertThat(result.score()).isEqualTo(10);
        assertThat(result.totalScore()).isEqualTo(10);
        assertThat(result.items().get(0).questionId()).isEqualTo(99L);
        assertThat(result.items().get(0).correct()).isTrue();
        assertThat(result.items().get(0).analysis()).contains("HashMap");
    }

    @Test
    void scoresMultipleChoiceAnswersIndependentOfOrderAndSeparator() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        repository.save(new Question(
                100L,
                "以下哪些属于 Java 集合接口？",
                QuestionType.MULTIPLE_CHOICE,
                Difficulty.BEGINNER,
                "集合框架",
                "A,C",
                "List 和 Set 属于集合接口。"
        ));
        PracticeController controller = new PracticeController(repository, new InMemoryPracticeStatsRepository());

        PracticeResult result = controller.submit(USER, new PracticeSubmitRequest(List.of(
                new PracticeAnswer(100L, " c 、 a ")
        ))).data();

        assertThat(result.score()).isEqualTo(10);
        assertThat(result.items().get(0).correct()).isTrue();
    }

    @Test
    void rejectsPracticeSubmissionWithoutAnswers() {
        PracticeController controller = controllerWithSampleQuestions();

        assertThatThrownBy(() -> controller.submit(USER, new PracticeSubmitRequest(List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("至少提交一道题目答案");
    }

    @Test
    void rejectsBlankPracticeAnswersWithoutRecordingStatsOrAttempts() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        repository.save(new Question(
                1L,
                "HashMap 默认负载因子是多少？",
                QuestionType.FILL_BLANK,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "0.75",
                "HashMap 默认负载因子是 0.75。"
        ));
        InMemoryPracticeStatsRepository stats = new InMemoryPracticeStatsRepository();
        InMemoryLearningAttemptRepository attempts = new InMemoryLearningAttemptRepository();
        PracticeController controller = new PracticeController(
                repository,
                stats,
                new PracticeGenerator(repository, new Random(1)),
                attempts,
                Clock.fixed(Instant.parse("2026-07-11T08:00:00Z"), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> controller.submit(USER, new PracticeSubmitRequest(List.of(
                new PracticeAnswer(1L, "   ")
        ))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("练习答案不能为空");
        assertThat(stats.findByUserId(USER.userId()).answeredQuestionCount()).isZero();
        assertThat(attempts.findByUserId(USER.userId())).isEmpty();
    }

    @Test
    void savesSubjectiveAnswersWithoutAutomaticScoring() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        repository.save(new Question(
                101L,
                "说明 ArrayList 与 LinkedList 的差异。",
                QuestionType.SHORT_ANSWER,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "ArrayList 基于数组，LinkedList 基于链表。",
                "应从随机访问和增删复杂度分析。"
        ));
        PracticeController controller = new PracticeController(repository, new InMemoryPracticeStatsRepository());

        PracticeResult result = controller.submit(USER, new PracticeSubmitRequest(List.of(
                new PracticeAnswer(101L, "我的理解")
        ))).data();

        assertThat(result.score()).isZero();
        assertThat(result.totalScore()).isZero();
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.autoGraded()).isFalse();
            assertThat(item.correct()).isNull();
            assertThat(item.correctAnswer()).contains("ArrayList 基于数组");
            assertThat(item.submittedAnswer()).isEqualTo("我的理解");
        });
        assertThat(controller.stats(USER).data().answeredQuestionCount()).isEqualTo(1);
        assertThat(controller.stats(USER).data().gradedQuestionCount()).isZero();
        assertThat(controller.stats(USER).data().correctQuestionCount()).isZero();
    }

    @Test
    void generatesFilteredPracticeWithoutReturningAnswersOrAnalysis() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        repository.save(new Question(
                201L,
                "HashMap 是否允许 null 键？\nA. 允许\nB. 不允许",
                QuestionType.SINGLE_CHOICE,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "A",
                "HashMap 允许一个 null 键。"
        ));
        PracticeController controller = new PracticeController(
                repository,
                new InMemoryPracticeStatsRepository(),
                new PracticeGenerator(repository, new Random(3))
        );

        GeneratedPractice generated = controller.generate(new PracticeGenerateRequest(
                "集合框架",
                Difficulty.INTERMEDIATE,
                QuestionType.SINGLE_CHOICE,
                5
        )).data();

        assertThat(generated.requestedCount()).isEqualTo(5);
        assertThat(generated.actualCount()).isEqualTo(1);
        assertThat(generated.questions()).singleElement().satisfies(question -> {
            assertThat(question.id()).isEqualTo(201L);
            assertThat(question.title()).contains("HashMap");
            assertThat(question.type()).isEqualTo(QuestionType.SINGLE_CHOICE);
        });
    }

    @Test
    void recordsTrustedObjectiveAndSubjectiveAttemptsFromQuestionBank() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        repository.save(new Question(
                301L,
                "HashMap 默认负载因子？",
                QuestionType.FILL_BLANK,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "0.75",
                "默认负载因子为 0.75。"
        ));
        repository.save(new Question(
                302L,
                "说明 JVM 堆的作用。",
                QuestionType.SHORT_ANSWER,
                Difficulty.ADVANCED,
                "JVM",
                "保存对象实例和数组。",
                "堆是线程共享区域。"
        ));
        InMemoryLearningAttemptRepository attempts = new InMemoryLearningAttemptRepository();
        PracticeController controller = new PracticeController(
                repository,
                new InMemoryPracticeStatsRepository(),
                new PracticeGenerator(repository, new Random(5)),
                attempts,
                Clock.fixed(Instant.parse("2026-07-11T08:00:00Z"), ZoneOffset.UTC)
        );

        controller.submit(USER, new PracticeSubmitRequest(List.of(
                new PracticeAnswer(301L, "0.75"),
                new PracticeAnswer(302L, "我的理解")
        )));

        assertThat(attempts.findByUserId(USER.userId())).hasSize(2);
        assertThat(attempts.findByUserId(USER.userId()).get(0)).satisfies(attempt -> {
            assertThat(attempt.questionTitle()).isEqualTo("HashMap 默认负载因子？");
            assertThat(attempt.questionType()).isEqualTo(QuestionType.FILL_BLANK);
            assertThat(attempt.knowledgePoint()).isEqualTo("集合框架");
            assertThat(attempt.autoGraded()).isTrue();
            assertThat(attempt.correct()).isTrue();
            assertThat(attempt.attemptedAt()).isEqualTo(Instant.parse("2026-07-11T08:00:00Z"));
        });
        assertThat(attempts.findByUserId(USER.userId()).get(1)).satisfies(attempt -> {
            assertThat(attempt.questionType()).isEqualTo(QuestionType.SHORT_ANSWER);
            assertThat(attempt.autoGraded()).isFalse();
            assertThat(attempt.correct()).isNull();
        });
    }

    @Test
    void listsRecentPracticeBatchesForCurrentUserWithoutCountingSubjectiveAccuracy() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        repository.save(new Question(
                401L,
                "JVM 堆中主要保存什么？",
                QuestionType.FILL_BLANK,
                Difficulty.INTERMEDIATE,
                "JVM",
                "对象实例",
                "JVM 堆保存对象实例。"
        ));
        repository.save(new Question(
                402L,
                "说明垃圾回收的目标。",
                QuestionType.SHORT_ANSWER,
                Difficulty.ADVANCED,
                "JVM",
                "回收不可达对象。",
                "主观题解析。"
        ));
        repository.save(new Question(
                403L,
                "ArrayList 属于哪个包？",
                QuestionType.FILL_BLANK,
                Difficulty.BEGINNER,
                "集合框架",
                "java.util",
                "ArrayList 位于 java.util 包。"
        ));
        InMemoryLearningAttemptRepository attempts = new InMemoryLearningAttemptRepository();
        PracticeController controller = new PracticeController(
                repository,
                new InMemoryPracticeStatsRepository(),
                new PracticeGenerator(repository, new Random(9)),
                attempts,
                Clock.fixed(Instant.parse("2026-07-12T01:00:00Z"), ZoneOffset.UTC)
        );

        controller.submit(USER, new PracticeSubmitRequest(List.of(
                new PracticeAnswer(401L, "对象实例"),
                new PracticeAnswer(402L, "我的理解")
        )));
        controller.submit(USER, new PracticeSubmitRequest(List.of(
                new PracticeAnswer(403L, "错误包名")
        )));
        controller.submit(OTHER_USER, new PracticeSubmitRequest(List.of(
                new PracticeAnswer(401L, "错误答案")
        )));

        List<RecentPracticeSummary> recent = controller.recent(USER, 5).data();

        assertThat(recent).hasSize(2);
        assertThat(recent.get(0)).satisfies(summary -> {
            assertThat(summary.answeredQuestionCount()).isEqualTo(1);
            assertThat(summary.gradedQuestionCount()).isEqualTo(1);
            assertThat(summary.correctQuestionCount()).isZero();
            assertThat(summary.accuracy()).isZero();
            assertThat(summary.knowledgePoints()).containsExactly("集合框架");
        });
        assertThat(recent.get(1)).satisfies(summary -> {
            assertThat(summary.answeredQuestionCount()).isEqualTo(2);
            assertThat(summary.gradedQuestionCount()).isEqualTo(1);
            assertThat(summary.correctQuestionCount()).isEqualTo(1);
            assertThat(summary.accuracy()).isEqualTo(1.0);
            assertThat(summary.knowledgePoints()).containsExactly("JVM");
        });
        assertThatThrownBy(() -> controller.recent(USER, 21))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("最近练习数量必须在 1 到 20 之间");
    }

    private PracticeController controllerWithSampleQuestions() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        repository.save(new Question(
                1L,
                "HashMap 默认负载因子是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "A",
                "HashMap 默认负载因子是 0.75，达到阈值后会触发扩容。"
        ));
        repository.save(new Question(
                2L,
                "Java 局部变量是否有默认值？",
                QuestionType.TRUE_FALSE,
                Difficulty.BEGINNER,
                "Java 基础",
                "true",
                "Java 基本类型局部变量没有默认值，必须先赋值再使用。"
        ));
        repository.save(new Question(
                3L,
                "ArrayList 何时扩容？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "A",
                "ArrayList 在容量不足以容纳新增元素时会触发扩容。"
        ));
        return new PracticeController(repository, new InMemoryPracticeStatsRepository());
    }
}
