package com.studycollection.question.api;

import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.app.QuestionFeedbackService;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.FeedbackStatus;
import com.studycollection.question.domain.FeedbackType;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionFeedbackControllerTest {
    @Test
    void userCanSubmitFeedbackAndAdminCanAcceptItToUpdateQuestion() {
        InMemoryQuestionRepository questionRepository = new InMemoryQuestionRepository();
        Question question = questionRepository.save(new Question(
                null,
                "Java 中 int 默认值是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "默认值是 1"
        ));
        QuestionFeedbackController controller = new QuestionFeedbackController(new QuestionFeedbackService(questionRepository));

        QuestionFeedback feedback = controller.submit(new SubmitFeedbackRequest(
                7L,
                question.id(),
                FeedbackType.ANSWER_ERROR,
                "标准答案应为 B"
        )).data();

        assertThat(feedback.status()).isEqualTo(FeedbackStatus.PENDING);

        List<QuestionFeedback> pending = controller.pending().data();

        assertThat(pending).extracting(QuestionFeedback::id).containsExactly(feedback.id());

        QuestionRevision revision = controller.accept(feedback.id(), new AcceptFeedbackRequest(
                1L,
                "将标准答案从 A 修订为 B",
                "用户反馈属实",
                "B",
                "Java 基本类型 int 的默认值是 0。"
        )).data();

        assertThat(revision.questionId()).isEqualTo(question.id());
        assertThat(questionRepository.findById(question.id()).answer()).isEqualTo("B");
        assertThat(questionRepository.findById(question.id()).analysis()).contains("默认值是 0");
        assertThat(controller.pending().data()).isEmpty();
    }

    @Test
    void adminCanRejectFeedbackOrMarkItNeedsReview() {
        QuestionFeedbackController controller = new QuestionFeedbackController();
        QuestionFeedback rejected = controller.submit(new SubmitFeedbackRequest(
                7L,
                101L,
                FeedbackType.ANSWER_ERROR,
                "标准答案应为 B"
        )).data();
        QuestionFeedback needsReview = controller.submit(new SubmitFeedbackRequest(
                8L,
                102L,
                FeedbackType.EXPLANATION_ERROR,
                "解析需要补充"
        )).data();

        QuestionFeedback rejectedResult = controller.reject(rejected.id(), new ReviewFeedbackRequest(
                1L,
                "原答案正确，驳回反馈"
        )).data();
        QuestionFeedback needsReviewResult = controller.markNeedsReview(needsReview.id(), new ReviewFeedbackRequest(
                1L,
                "交给教研复核"
        )).data();

        assertThat(rejectedResult.status()).isEqualTo(FeedbackStatus.REJECTED);
        assertThat(needsReviewResult.status()).isEqualTo(FeedbackStatus.NEEDS_REVIEW);
        assertThat(controller.pending().data()).isEmpty();
    }
}
