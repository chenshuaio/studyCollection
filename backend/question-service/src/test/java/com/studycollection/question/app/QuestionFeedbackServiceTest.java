package com.studycollection.question.app;

import com.studycollection.question.domain.FeedbackStatus;
import com.studycollection.question.domain.FeedbackType;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionFeedbackServiceTest {
    @Test
    void acceptedFeedbackCreatesRevisionHistory() {
        QuestionFeedbackService service = new QuestionFeedbackService();
        QuestionFeedback feedback = service.submit(
                7L,
                101L,
                FeedbackType.ANSWER_ERROR,
                "标准答案应为 B，当前答案 A 不正确"
        );

        QuestionRevision revision = service.accept(
                feedback.id(),
                1L,
                "答案从 A 修改为 B",
                "用户反馈属实"
        );

        assertThat(service.find(feedback.id()).status()).isEqualTo(FeedbackStatus.ACCEPTED);
        assertThat(revision.questionId()).isEqualTo(101L);
        assertThat(revision.changeSummary()).contains("答案从 A 修改为 B");
    }

    @Test
    void adminCanRejectFeedbackOrMarkItNeedsReview() {
        QuestionFeedbackService service = new QuestionFeedbackService();
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
}
