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
}
