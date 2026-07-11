package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.FeedbackStatus;
import com.studycollection.question.domain.FeedbackType;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionFeedbackServiceTest {
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
}
