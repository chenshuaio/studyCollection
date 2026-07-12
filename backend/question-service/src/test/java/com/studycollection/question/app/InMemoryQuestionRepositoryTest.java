package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionBankScope;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void limitsAccessibleQuestionsToPublicAndCurrentUsersPersonalBank() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        Question publicQuestion = repository.save(new Question(
                null, "公共题", QuestionType.SINGLE_CHOICE, Difficulty.BEGINNER,
                "Java 基础", "A", "公共解析"
        ));
        Question ownQuestion = repository.save(new Question(
                null, 7L, "我的个人题", QuestionType.SHORT_ANSWER, Difficulty.INTERMEDIATE,
                "集合框架", "答案", "个人解析"
        ));
        Question foreignQuestion = repository.save(new Question(
                null, 8L, "他人的个人题", QuestionType.PROGRAMMING, Difficulty.ADVANCED,
                "并发编程", "代码", "他人解析"
        ));

        assertThat(repository.searchAccessible(7L, QuestionBankScope.ALL, null, null, null, null))
                .extracting(Question::id)
                .containsExactly(publicQuestion.id(), ownQuestion.id());
        assertThat(repository.searchAccessible(7L, QuestionBankScope.PUBLIC, null, null, null, null))
                .extracting(Question::id)
                .containsExactly(publicQuestion.id());
        assertThat(repository.searchAccessible(7L, QuestionBankScope.PERSONAL, null, null, null, null))
                .extracting(Question::id)
                .containsExactly(ownQuestion.id());
        assertThatThrownBy(() -> repository.findAccessibleById(foreignQuestion.id(), 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("题目不存在或无权访问");
    }

    @Test
    void onlyDeletesQuestionsOwnedByCurrentUser() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        Question publicQuestion = repository.save(new Question(
                null, "公共题", QuestionType.SINGLE_CHOICE, Difficulty.BEGINNER,
                "Java 基础", "A", "公共解析"
        ));
        Question ownQuestion = repository.save(new Question(
                null, 7L, "我的个人题", QuestionType.SINGLE_CHOICE, Difficulty.BEGINNER,
                "Java 基础", "A", "个人解析"
        ));
        Question foreignQuestion = repository.save(new Question(
                null, 8L, "他人的个人题", QuestionType.SINGLE_CHOICE, Difficulty.BEGINNER,
                "Java 基础", "A", "他人解析"
        ));

        repository.deleteOwnedById(ownQuestion.id(), 7L);

        assertThat(repository.search(null, null, null, null)).extracting(Question::id)
                .containsExactly(publicQuestion.id(), foreignQuestion.id());
        assertThatThrownBy(() -> repository.deleteOwnedById(publicQuestion.id(), 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("题目不存在或无权访问");
        assertThatThrownBy(() -> repository.deleteOwnedById(foreignQuestion.id(), 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("题目不存在或无权访问");
    }
}
