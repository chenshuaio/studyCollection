package com.studycollection.exam.app;

import com.studycollection.exam.api.CustomExamRequest;
import com.studycollection.exam.api.PracticeStats;
import com.studycollection.exam.domain.ExamSession;
import com.studycollection.exam.domain.ExamStatus;
import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExamSessionServiceTest {
    private MutableClock clock;
    private InMemoryExamSessionRepository sessionRepository;
    private InMemoryPracticeStatsRepository statsRepository;
    private InMemoryLearningAttemptRepository attemptRepository;
    private ExamSessionService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-07-11T05:00:00Z"));
        sessionRepository = new InMemoryExamSessionRepository();
        statsRepository = new InMemoryPracticeStatsRepository();
        attemptRepository = new InMemoryLearningAttemptRepository();
        service = new ExamSessionService(
                sessionRepository,
                sampleQuestions(),
                statsRepository,
                attemptRepository,
                clock
        );
    }

    @Test
    void createsSnapshotsInSelectedOrderAndListsOnlyOwnerHistory() {
        ExamSession created = service.create(7L, new CustomExamRequest(
                "综合测试",
                30,
                List.of(2L, 1L)
        ));

        assertThat(created.id()).isPositive();
        assertThat(created.questions()).extracting(question -> question.questionId())
                .containsExactly(2L, 1L);
        assertThat(created.questions().get(1).correctAnswer()).isEqualTo("A");
        assertThat(service.list(7L)).extracting(ExamSession::id).containsExactly(created.id());
        assertThat(service.list(8L)).isEmpty();
    }

    @Test
    void rejectsDuplicateQuestionsAndForeignOwnership() {
        assertThatThrownBy(() -> service.create(7L, new CustomExamRequest(
                "重复题目",
                30,
                List.of(1L, 1L)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("题目不能重复选择");

        assertThatThrownBy(() -> service.create(7L, new CustomExamRequest(
                "越权个人题",
                30,
                List.of(3L)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("题目不存在或无权访问");

        ExamSession personal = service.create(7L, new CustomExamRequest("我的个人题试卷", 30, List.of(4L)));
        assertThat(personal.questions()).extracting(question -> question.questionId()).containsExactly(4L);

        ExamSession created = service.create(7L, new CustomExamRequest("私有试卷", 30, List.of(1L)));
        assertThatThrownBy(() -> service.get(8L, created.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("考试记录不存在");
    }

    @Test
    void savesAnswersSubmitsOnceAndUpdatesRealStatsOnce() {
        ExamSession created = service.create(7L, new CustomExamRequest(
                "评分测试",
                30,
                List.of(1L, 2L)
        ));

        service.saveAnswer(7L, created.id(), 1L, "A");
        service.saveAnswer(7L, created.id(), 2L, "我的解释");
        ExamSession submitted = service.submit(7L, created.id());
        ExamSession repeated = service.submit(7L, created.id());

        assertThat(submitted.status()).isEqualTo(ExamStatus.SUBMITTED);
        assertThat(submitted.score()).isEqualTo(10);
        assertThat(submitted.totalScore()).isEqualTo(10);
        assertThat(submitted.answers().get(2L).autoGraded()).isFalse();
        assertThat(repeated).isEqualTo(submitted);
        PracticeStats stats = statsRepository.findByUserId(7L);
        assertThat(stats.answeredQuestionCount()).isEqualTo(2);
        assertThat(stats.gradedQuestionCount()).isEqualTo(1);
        assertThat(stats.correctQuestionCount()).isEqualTo(1);
        assertThat(attemptRepository.findByUserId(7L)).hasSize(2);
        assertThat(attemptRepository.findByUserId(7L)).allSatisfy(attempt -> {
            assertThat(attempt.referenceId()).isEqualTo(String.valueOf(created.id()));
            assertThat(attempt.activityType()).isEqualTo(LearningActivityType.EXAM);
            assertThat(attempt.attemptedAt()).isEqualTo(clock.instant());
        });
        assertThat(attemptRepository.findByUserId(7L).get(1).autoGraded()).isFalse();
    }

    @Test
    void finalizesExpiredSessionWithOnlyPreviouslySavedAnswers() {
        ExamSession created = service.create(7L, new CustomExamRequest(
                "一分钟测试",
                1,
                List.of(1L)
        ));
        service.saveAnswer(7L, created.id(), 1L, "B");
        clock.advanceSeconds(61);

        ExamSession expired = service.get(7L, created.id());
        ExamSession unchanged = service.saveAnswer(7L, created.id(), 1L, "A");

        assertThat(expired.status()).isEqualTo(ExamStatus.SUBMITTED);
        assertThat(expired.answers().get(1L).submittedAnswer()).isEqualTo("B");
        assertThat(expired.answers().get(1L).correct()).isFalse();
        assertThat(unchanged).isEqualTo(expired);
        assertThat(statsRepository.findByUserId(7L).answeredQuestionCount()).isEqualTo(1);
        assertThat(statsRepository.findByUserId(7L).gradedQuestionCount()).isEqualTo(1);
    }

    private InMemoryQuestionRepository sampleQuestions() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        repository.save(new Question(
                1L,
                "Java 中 int 默认值是多少？\nA. 0\nB. null",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "成员变量的 int 默认值为 0。"
        ));
        repository.save(new Question(
                2L,
                "说明 ArrayList 与 LinkedList 的差异。",
                QuestionType.SHORT_ANSWER,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "ArrayList 基于数组，LinkedList 基于链表。",
                "需要从访问和增删复杂度分析。"
        ));
        repository.save(new Question(
                3L,
                8L,
                "他人的个人题",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "他人解析"
        ));
        repository.save(new Question(
                4L,
                7L,
                "我的个人题",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "个人解析"
        ));
        return repository;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
