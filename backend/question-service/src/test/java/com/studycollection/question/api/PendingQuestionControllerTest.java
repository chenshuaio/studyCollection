package com.studycollection.question.api;

import com.studycollection.question.app.InMemoryPendingQuestionRepository;
import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.PendingQuestion;
import com.studycollection.question.domain.PendingQuestionStatus;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PendingQuestionControllerTest {
    @Test
    void submitsApprovesAndRejectsImportedQuestions() {
        InMemoryQuestionRepository questionRepository = new InMemoryQuestionRepository();
        PendingQuestionController controller = new PendingQuestionController(
                new InMemoryPendingQuestionRepository(),
                questionRepository
        );

        PendingQuestion submitted = controller.submit(new SubmitPendingQuestionRequest(
                7L,
                "HashMap 默认负载因子是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "A",
                "由导入提交，等待管理员审核"
        )).data();
        PendingQuestion rejected = controller.submit(new SubmitPendingQuestionRequest(
                7L,
                "错误题目",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "B",
                "需要拒绝"
        )).data();

        assertThat(submitted.status()).isEqualTo(PendingQuestionStatus.PENDING);
        assertThat(controller.pending().data()).extracting(PendingQuestion::id)
                .containsExactly(submitted.id(), rejected.id());

        Question approved = controller.approve(submitted.id()).data();
        PendingQuestion rejectedResult = controller.reject(rejected.id()).data();
        List<Question> formalQuestions = questionRepository.search("HashMap", null, null, null);

        assertThat(approved.title()).isEqualTo("HashMap 默认负载因子是多少？");
        assertThat(rejectedResult.status()).isEqualTo(PendingQuestionStatus.REJECTED);
        assertThat(formalQuestions).extracting(Question::title)
                .containsExactly("HashMap 默认负载因子是多少？");
        assertThat(controller.pending().data()).isEmpty();
    }
}
