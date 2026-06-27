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
}
