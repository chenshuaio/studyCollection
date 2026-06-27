package com.studycollection.question.api;

import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionControllerTest {
    @Test
    void createsAndSearchesQuestions() {
        QuestionController controller = new QuestionController(new InMemoryQuestionRepository());

        Question created = controller.create(new CreateQuestionRequest(
                "Java 中 HashMap 默认负载因子是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "0.75",
                "HashMap 默认负载因子是 0.75。"
        )).data();

        List<Question> result = controller.search("集合框架", Difficulty.INTERMEDIATE, QuestionType.SINGLE_CHOICE).data();

        assertThat(created.id()).isEqualTo(1L);
        assertThat(result).extracting(Question::title)
                .containsExactly("Java 中 HashMap 默认负载因子是多少？");
    }
}
