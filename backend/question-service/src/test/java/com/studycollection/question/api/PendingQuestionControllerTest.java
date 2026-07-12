package com.studycollection.question.api;

import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.common.security.Role;
import com.studycollection.question.app.InMemoryPendingQuestionRepository;
import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.PendingQuestion;
import com.studycollection.question.domain.PendingQuestionStatus;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionBankScope;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PendingQuestionControllerTest {
    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "alice", Role.USER);

    @Test
    void submitsApprovesAndRejectsImportedQuestions() {
        InMemoryQuestionRepository questionRepository = new InMemoryQuestionRepository();
        PendingQuestionController controller = new PendingQuestionController(
                new InMemoryPendingQuestionRepository(),
                questionRepository
        );

        PendingQuestion submitted = controller.submit(USER, new SubmitPendingQuestionRequest(
                "HashMap 默认负载因子是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "A",
                "由导入提交，等待管理员审核",
                QuestionBankScope.PERSONAL
        )).data();
        PendingQuestion publicSubmission = controller.submit(USER, new SubmitPendingQuestionRequest(
                "Java 中 int 默认值是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "申请进入公共题库",
                QuestionBankScope.PUBLIC
        )).data();
        PendingQuestion rejected = controller.submit(USER, new SubmitPendingQuestionRequest(
                "错误题目",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "B",
                "需要拒绝"
        )).data();

        assertThat(submitted.status()).isEqualTo(PendingQuestionStatus.PENDING);
        assertThat(submitted.submitterUserId()).isEqualTo(USER.userId());
        assertThat(submitted.targetScope()).isEqualTo(QuestionBankScope.PERSONAL);
        assertThat(controller.pending().data()).extracting(PendingQuestion::id)
                .containsExactly(submitted.id(), publicSubmission.id(), rejected.id());

        Question approved = controller.approve(submitted.id()).data();
        Question approvedPublic = controller.approve(publicSubmission.id()).data();
        PendingQuestion rejectedResult = controller.reject(rejected.id()).data();
        List<Question> formalQuestions = questionRepository.search(null, null, null, null);

        assertThat(approved.title()).isEqualTo("HashMap 默认负载因子是多少？");
        assertThat(approved.ownerUserId()).isEqualTo(USER.userId());
        assertThat(approvedPublic.ownerUserId()).isNull();
        assertThat(rejectedResult.status()).isEqualTo(PendingQuestionStatus.REJECTED);
        assertThat(formalQuestions).extracting(Question::title)
                .containsExactly("HashMap 默认负载因子是多少？", "Java 中 int 默认值是多少？");
        assertThat(controller.pending().data()).isEmpty();
    }

    @Test
    void processedPendingQuestionCannotBeApprovedTwice() {
        InMemoryQuestionRepository questionRepository = new InMemoryQuestionRepository();
        PendingQuestionController controller = new PendingQuestionController(
                new InMemoryPendingQuestionRepository(),
                questionRepository
        );
        PendingQuestion submitted = controller.submit(USER, new SubmitPendingQuestionRequest(
                "Java 中 int 默认值是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "成员变量默认值为 0"
        )).data();

        assertThat(submitted.targetScope()).isEqualTo(QuestionBankScope.PUBLIC);

        controller.approve(submitted.id());

        assertThatThrownBy(() -> controller.approve(submitted.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("待审核题目已处理");
        assertThat(questionRepository.search(null, null, null, null)).hasSize(1);
    }
}
