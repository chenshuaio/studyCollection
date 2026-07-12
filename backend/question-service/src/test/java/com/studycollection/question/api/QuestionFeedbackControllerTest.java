package com.studycollection.question.api;

import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.common.security.Role;
import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.app.InMemoryQuestionFeedbackRepository;
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
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1L, "admin", Role.ADMIN);
    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "alice", Role.USER);
    private static final AuthenticatedUser OTHER_USER = new AuthenticatedUser(8L, "bob", Role.USER);

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
        QuestionFeedbackController controller = new QuestionFeedbackController(new QuestionFeedbackService(
                new InMemoryQuestionFeedbackRepository(),
                questionRepository
        ));

        QuestionFeedback feedback = controller.submit(USER, new SubmitFeedbackRequest(
                question.id(),
                FeedbackType.ANSWER_ERROR,
                "标准答案应为 B"
        )).data();

        assertThat(feedback.status()).isEqualTo(FeedbackStatus.PENDING);

        List<QuestionFeedback> pending = controller.pending().data();

        assertThat(pending).extracting(QuestionFeedback::id).containsExactly(feedback.id());

        QuestionRevision revision = controller.accept(ADMIN, feedback.id(), new AcceptFeedbackRequest(
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
        InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
        questions.save(new Question(
                101L,
                "测试公共题 101",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "解析"
        ));
        questions.save(new Question(
                102L,
                "测试公共题 102",
                QuestionType.SHORT_ANSWER,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "答案",
                "解析"
        ));
        QuestionFeedbackController controller = new QuestionFeedbackController(new QuestionFeedbackService(
                new InMemoryQuestionFeedbackRepository(),
                questions
        ));
        QuestionFeedback rejected = controller.submit(USER, new SubmitFeedbackRequest(
                101L,
                FeedbackType.ANSWER_ERROR,
                "标准答案应为 B"
        )).data();
        QuestionFeedback needsReview = controller.submit(OTHER_USER, new SubmitFeedbackRequest(
                102L,
                FeedbackType.EXPLANATION_ERROR,
                "解析需要补充"
        )).data();

        QuestionFeedback rejectedResult = controller.reject(ADMIN, rejected.id(), new ReviewFeedbackRequest(
                "原答案正确，驳回反馈"
        )).data();
        QuestionFeedback needsReviewResult = controller.markNeedsReview(ADMIN, needsReview.id(), new ReviewFeedbackRequest(
                "交给教研复核"
        )).data();

        assertThat(rejectedResult.status()).isEqualTo(FeedbackStatus.REJECTED);
        assertThat(needsReviewResult.status()).isEqualTo(FeedbackStatus.NEEDS_REVIEW);
        assertThat(controller.pending().data()).isEmpty();
    }
}
