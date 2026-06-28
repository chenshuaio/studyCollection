package com.studycollection.question.api;

import com.studycollection.question.domain.FeedbackStatus;
import com.studycollection.question.domain.FeedbackType;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionFeedbackControllerTest {
    @Test
    void userCanSubmitFeedbackAndAdminCanAcceptIt() {
        QuestionFeedbackController controller = new QuestionFeedbackController();

        QuestionFeedback feedback = controller.submit(new SubmitFeedbackRequest(
                7L,
                101L,
                FeedbackType.ANSWER_ERROR,
                "标准答案应为 B"
        )).data();

        assertThat(feedback.status()).isEqualTo(FeedbackStatus.PENDING);

        List<QuestionFeedback> pending = controller.pending().data();

        assertThat(pending).extracting(QuestionFeedback::id).containsExactly(feedback.id());

        QuestionRevision revision = controller.accept(feedback.id(), new AcceptFeedbackRequest(
                1L,
                "将标准答案从 A 修订为 B",
                "用户反馈属实"
        )).data();

        assertThat(revision.questionId()).isEqualTo(101L);
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
