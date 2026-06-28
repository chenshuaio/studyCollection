package com.studycollection.question.api;

import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionControllerTest {
    @Test
    void createsAndSearchesQuestionsWithOptionalFilters() {
        QuestionController controller = new QuestionController(new InMemoryQuestionRepository());

        Question hashMap = controller.create(new CreateQuestionRequest(
                "Java 中 HashMap 默认负载因子是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "0.75",
                "HashMap 默认负载因子是 0.75。"
        )).data();
        Question jvm = controller.create(new CreateQuestionRequest(
                "JVM 栈内存主要保存什么？",
                QuestionType.SHORT_ANSWER,
                Difficulty.BEGINNER,
                "JVM",
                "栈帧",
                "虚拟机栈保存方法调用的栈帧。"
        )).data();

        List<Question> all = controller.search(null, null, null, null).data();
        List<Question> fuzzy = controller.search("HashMap", null, null, null).data();
        List<Question> filtered = controller.search(null, "集合框架", Difficulty.INTERMEDIATE, QuestionType.SINGLE_CHOICE).data();

        assertThat(hashMap.id()).isEqualTo(1L);
        assertThat(jvm.id()).isEqualTo(2L);
        assertThat(all).extracting(Question::id).containsExactly(1L, 2L);
        assertThat(fuzzy).extracting(Question::id).containsExactly(1L);
        assertThat(filtered).extracting(Question::id).containsExactly(1L);
    }

    @Test
    void deletesQuestionsFromFormalQuestionBank() {
        QuestionController controller = new QuestionController(new InMemoryQuestionRepository());

        Question hashMap = controller.create(new CreateQuestionRequest(
                "HashMap 默认负载因子是多少？",
                QuestionType.SINGLE_CHOICE,
                Difficulty.INTERMEDIATE,
                "集合框架",
                "0.75",
                "HashMap 默认负载因子是 0.75。"
        )).data();
        Question jvm = controller.create(new CreateQuestionRequest(
                "JVM 栈内存主要保存什么？",
                QuestionType.SHORT_ANSWER,
                Difficulty.BEGINNER,
                "JVM",
                "栈帧",
                "虚拟机栈保存方法调用的栈帧。"
        )).data();

        Long deletedId = controller.deleteQuestion(hashMap.id()).data();
        List<Question> all = controller.search(null, null, null, null).data();

        assertThat(deletedId).isEqualTo(hashMap.id());
        assertThat(all).extracting(Question::id).containsExactly(jvm.id());
    }

    @Test
    void deleteEndpointNamesPathVariableExplicitly() throws NoSuchMethodException {
        Method method = QuestionController.class.getMethod("deleteQuestion", Long.class);
        PathVariable pathVariable = method.getParameters()[0].getAnnotation(PathVariable.class);

        assertThat(pathVariable.value()).isEqualTo("id");
    }
}
