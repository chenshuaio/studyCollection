package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.FeedbackStatus;
import com.studycollection.question.domain.FeedbackType;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionFeedbackServiceTest {
    @Test
    void groupsDuplicatePendingAndNeedsReviewFeedbackWithUserAnswerAndSource() {
        InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
        Question question = questions.save(new Question(
                null,
                "Java 中 int 成员变量默认值是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "成员变量默认值为 0。"
        ));
        InMemoryQuestionFeedbackRepository feedbacks = new InMemoryQuestionFeedbackRepository();
        QuestionFeedbackService firstService = service(
                feedbacks,
                questions,
                Instant.parse("2026-07-10T08:00:00Z")
        );
        QuestionFeedback first = firstService.submit(
                7L,
                question.id(),
                FeedbackType.ANSWER_ERROR,
                "标准答案应为 B",
                "B",
                "PRACTICE",
                "practice-91"
        );
        QuestionFeedbackService secondService = service(
                feedbacks,
                questions,
                Instant.parse("2026-07-12T09:00:00Z")
        );
        QuestionFeedback second = secondService.submit(
                8L,
                question.id(),
                FeedbackType.ANSWER_ERROR,
                "答案 A 不正确",
                "B",
                "EXAM",
                "exam-12"
        );
        secondService.markNeedsReview(second.id(), 1L, "交给教研复核");

        List<QuestionFeedbackGroup> groups = secondService.pendingGroups();

        assertThat(groups).singleElement().satisfies(group -> {
            assertThat(group.questionId()).isEqualTo(question.id());
            assertThat(group.questionTitle()).contains("int 成员变量");
            assertThat(group.questionSource()).isEqualTo("LOCAL_UPLOAD");
            assertThat(group.type()).isEqualTo(FeedbackType.ANSWER_ERROR);
            assertThat(group.feedbackCount()).isEqualTo(2);
            assertThat(group.latestAt()).isEqualTo(Instant.parse("2026-07-12T09:00:00Z"));
            assertThat(group.items()).extracting(QuestionFeedback::id)
                    .containsExactly(second.id(), first.id());
            assertThat(group.items().get(0).submittedAnswer()).isEqualTo("B");
            assertThat(group.items().get(0).sourceContext()).isEqualTo("EXAM");
            assertThat(group.items().get(0).sourceReference()).isEqualTo("exam-12");
        });
    }

    @Test
    void acceptingDuplicateGroupPersistsReviewAuditAndCompleteRevisionSnapshots() {
        InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
        Question question = questions.save(new Question(
                null,
                "Java 中 int 成员变量默认值是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "旧解析"
        ));
        InMemoryQuestionFeedbackRepository feedbacks = new InMemoryQuestionFeedbackRepository();
        QuestionFeedbackService service = service(
                feedbacks,
                questions,
                Instant.parse("2026-07-12T10:00:00Z")
        );
        QuestionFeedback first = service.submit(7L, question.id(), FeedbackType.ANSWER_ERROR, "应为 B");
        QuestionFeedback second = service.submit(8L, question.id(), FeedbackType.ANSWER_ERROR, "答案标错了");

        QuestionRevision revision = service.acceptGroup(
                List.of(first.id(), second.id()),
                1L,
                "标准答案从 A 修改为 B",
                "两位用户反馈一致，已核验",
                "B",
                "int 成员变量默认值是 0。"
        );

        assertThat(revision.feedbackId()).isEqualTo(first.id());
        assertThat(revision.relatedFeedbackIds()).containsExactly(first.id(), second.id());
        assertThat(revision.beforeQuestion().answer()).isEqualTo("A");
        assertThat(revision.afterQuestion().answer()).isEqualTo("B");
        assertThat(revision.beforeQuestion().analysis()).isEqualTo("旧解析");
        assertThat(revision.afterQuestion().analysis()).contains("默认值是 0");
        assertThat(revision.scoringAffected()).isTrue();
        assertThat(revision.revisedAt()).isEqualTo(Instant.parse("2026-07-12T10:00:00Z"));
        assertThat(service.find(first.id()).status()).isEqualTo(FeedbackStatus.ACCEPTED);
        assertThat(service.find(second.id()).status()).isEqualTo(FeedbackStatus.ACCEPTED);
        assertThat(service.find(second.id()).reviewNote()).contains("已核验");
        assertThat(service.find(second.id()).reviewedBy()).isEqualTo(1L);
        assertThat(service.find(second.id()).reviewedAt()).isEqualTo(Instant.parse("2026-07-12T10:00:00Z"));
        assertThat(service.revisions(question.id())).containsExactly(revision);
        assertThat(feedbacks.findScoringAffectedQuestionIds()).containsExactly(question.id());
    }

    @Test
    void acceptingFeedbackCanReviseStemTypeDifficultyAndKnowledgePoint() {
        InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
        Question question = questions.save(new Question(
                null,
                "旧题干",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "旧解析"
        ));
        InMemoryQuestionFeedbackRepository feedbacks = new InMemoryQuestionFeedbackRepository();
        QuestionFeedbackService service = service(
                feedbacks,
                questions,
                Instant.parse("2026-07-12T11:00:00Z")
        );
        QuestionFeedback feedback = service.submit(
                7L,
                question.id(),
                FeedbackType.STEM_ERROR,
                "题干和分类都需要修订"
        );

        QuestionRevision revision = service.acceptGroup(
                List.of(feedback.id()),
                1L,
                "修订题干与分类",
                "已核验",
                "新题干\nA. 选项一\nB. 选项二",
                QuestionType.MULTIPLE_CHOICE,
                Difficulty.ADVANCED,
                "集合框架",
                "AB",
                "新解析"
        );

        assertThat(revision.beforeQuestion().title()).isEqualTo("旧题干");
        assertThat(revision.afterQuestion()).satisfies(snapshot -> {
            assertThat(snapshot.title()).contains("新题干", "选项一");
            assertThat(snapshot.type()).isEqualTo(QuestionType.MULTIPLE_CHOICE);
            assertThat(snapshot.difficulty()).isEqualTo(Difficulty.ADVANCED);
            assertThat(snapshot.knowledgePoint()).isEqualTo("集合框架");
            assertThat(snapshot.answer()).isEqualTo("AB");
        });
        assertThat(revision.scoringAffected()).isFalse();
    }

    @Test
    void acceptedFeedbackUpdatesQuestionAndCreatesRevisionHistory() {
        InMemoryQuestionRepository questionRepository = new InMemoryQuestionRepository();
        Question savedQuestion = questionRepository.save(new Question(
                null,
                "Java 中 int 默认值是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "默认值是 1"
        ));
        QuestionFeedbackService service = new QuestionFeedbackService(
                new InMemoryQuestionFeedbackRepository(),
                questionRepository
        );
        QuestionFeedback feedback = service.submit(
                7L,
                savedQuestion.id(),
                FeedbackType.ANSWER_ERROR,
                "标准答案应为 B，当前答案 A 不正确"
        );

        QuestionRevision revision = service.accept(
                feedback.id(),
                1L,
                "答案从 A 修改为 B",
                "用户反馈属实",
                "B",
                "Java 基本类型 int 的默认值是 0。"
        );

        assertThat(service.find(feedback.id()).status()).isEqualTo(FeedbackStatus.ACCEPTED);
        assertThat(revision.questionId()).isEqualTo(savedQuestion.id());
        assertThat(revision.changeSummary()).contains("答案从 A 修改为 B");
        Question revisedQuestion = questionRepository.findById(savedQuestion.id());
        assertThat(revisedQuestion.answer()).isEqualTo("B");
        assertThat(revisedQuestion.analysis()).contains("默认值是 0");
    }

    @Test
    void adminCanRejectFeedbackOrMarkItNeedsReview() {
        QuestionFeedbackService service = service();
        QuestionFeedback rejected = service.submit(
                7L,
                101L,
                FeedbackType.ANSWER_ERROR,
                "题目答案无误，用户理解有偏差"
        );
        QuestionFeedback needsReview = service.submit(
                8L,
                102L,
                FeedbackType.EXPLANATION_ERROR,
                "解析可能遗漏边界条件"
        );

        service.reject(rejected.id(), 1L, "核对题库后确认原答案正确");
        service.markNeedsReview(needsReview.id(), 1L, "需要教研二次确认");

        assertThat(service.find(rejected.id()).status()).isEqualTo(FeedbackStatus.REJECTED);
        assertThat(service.find(needsReview.id()).status()).isEqualTo(FeedbackStatus.NEEDS_REVIEW);
        assertThat(service.pending()).isEmpty();
    }

    @Test
    void listsFeedbackSubmittedByUser() {
        QuestionFeedbackService service = service();

        service.submit(7L, 101L, FeedbackType.ANSWER_ERROR, "标准答案应为 B");
        service.submit(8L, 102L, FeedbackType.EXPLANATION_ERROR, "解析需要补充");

        assertThat(service.byUser(7L)).hasSize(1);
        assertThat(service.byUser(7L).get(0).content()).contains("标准答案");
    }

    @Test
    void completedFeedbackCannotBeReviewedAgain() {
        InMemoryQuestionRepository questionRepository = new InMemoryQuestionRepository();
        Question question = questionRepository.save(new Question(
                null,
                "Java 中 int 默认值是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "成员变量默认值为 0"
        ));
        QuestionFeedbackService service = new QuestionFeedbackService(
                new InMemoryQuestionFeedbackRepository(),
                questionRepository
        );
        QuestionFeedback feedback = service.submit(
                7L,
                question.id(),
                FeedbackType.ANSWER_ERROR,
                "标准答案需要复核"
        );
        service.accept(feedback.id(), 1L, "确认无误", "已复核", null, null);

        assertThatThrownBy(() -> service.reject(feedback.id(), 1L, "重复处理"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("反馈已处理");
    }

    private QuestionFeedbackService service() {
        return new QuestionFeedbackService(
                new InMemoryQuestionFeedbackRepository(),
                new InMemoryQuestionRepository()
        );
    }

    private QuestionFeedbackService service(
            InMemoryQuestionFeedbackRepository feedbacks,
            InMemoryQuestionRepository questions,
            Instant now
    ) {
        return new QuestionFeedbackService(
                feedbacks,
                questions,
                Clock.fixed(now, ZoneOffset.UTC)
        );
    }
}
