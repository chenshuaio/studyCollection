package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryQuestionRepositoryTest {
    @Test
    void searchesAllQuestionsAndFuzzyMatchesTitle() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        repository.save(new Question(null, "HashMap 默认负载因子是多少？", QuestionType.SINGLE_CHOICE, Difficulty.INTERMEDIATE, "集合框架", "A", "0.75"));
        repository.save(new Question(null, "JVM 栈内存保存什么？", QuestionType.SHORT_ANSWER, Difficulty.BEGINNER, "JVM", "栈帧", "方法调用信息"));

        assertThat(repository.search(null, null, null, null)).extracting(Question::title)
                .containsExactly("HashMap 默认负载因子是多少？", "JVM 栈内存保存什么？");
        assertThat(repository.search("map", null, null, null)).extracting(Question::title)
                .containsExactly("HashMap 默认负载因子是多少？");
        assertThat(repository.search(null, "JVM", null, null)).extracting(Question::title)
                .containsExactly("JVM 栈内存保存什么？");
    }
}
