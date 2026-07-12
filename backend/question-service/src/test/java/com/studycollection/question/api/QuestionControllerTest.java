package com.studycollection.question.api;

import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.common.security.Role;
import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionBankScope;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionControllerTest {
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1L, "admin", Role.ADMIN);
    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "alice", Role.USER);
    private static final AuthenticatedUser OTHER_USER = new AuthenticatedUser(8L, "bob", Role.USER);

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

        List<Question> all = controller.search(ADMIN, null, null, null, null, QuestionBankScope.ALL).data();
        List<Question> fuzzy = controller.search(ADMIN, "HashMap", null, null, null, QuestionBankScope.ALL).data();
        List<Question> filtered = controller.search(
                ADMIN,
                null,
                "集合框架",
                Difficulty.INTERMEDIATE,
                QuestionType.SINGLE_CHOICE,
                QuestionBankScope.ALL
        ).data();

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

        Long deletedId = controller.deleteQuestion(ADMIN, hashMap.id()).data();
        List<Question> all = controller.search(ADMIN, null, null, null, null, QuestionBankScope.ALL).data();

        assertThat(deletedId).isEqualTo(hashMap.id());
        assertThat(all).extracting(Question::id).containsExactly(jvm.id());
    }

    @Test
    void deleteEndpointNamesPathVariableExplicitly() throws NoSuchMethodException {
        Method method = QuestionController.class.getMethod("deleteQuestion", AuthenticatedUser.class, Long.class);
        PathVariable pathVariable = method.getParameters()[1].getAnnotation(PathVariable.class);

        assertThat(pathVariable.value()).isEqualTo("id");
    }

    @Test
    void usersOnlySearchPublicAndOwnQuestionsWithoutAnswers() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        Question publicQuestion = repository.save(new Question(
                null, "公共题", QuestionType.SINGLE_CHOICE, Difficulty.BEGINNER,
                "Java 基础", "A", "公共解析"
        ));
        Question ownQuestion = repository.save(new Question(
                null, USER.userId(), "我的个人题", QuestionType.SHORT_ANSWER, Difficulty.INTERMEDIATE,
                "集合框架", "个人答案", "个人解析"
        ));
        repository.save(new Question(
                null, OTHER_USER.userId(), "他人的个人题", QuestionType.PROGRAMMING, Difficulty.ADVANCED,
                "并发编程", "他人答案", "他人解析"
        ));
        QuestionController controller = new QuestionController(repository);

        List<Question> all = controller.search(USER, null, null, null, null, QuestionBankScope.ALL).data();
        List<Question> personal = controller.search(
                USER, null, null, null, null, QuestionBankScope.PERSONAL
        ).data();

        assertThat(all).extracting(Question::id).containsExactly(publicQuestion.id(), ownQuestion.id());
        assertThat(all).allSatisfy(question -> {
            assertThat(question.answer()).isEmpty();
            assertThat(question.analysis()).isEmpty();
        });
        assertThat(personal).extracting(Question::id).containsExactly(ownQuestion.id());
    }

    @Test
    void userOnlyDeletesOwnPersonalQuestionWhileAdminDeletesPublicQuestion() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        Question publicQuestion = repository.save(new Question(
                null, "公共题", QuestionType.SINGLE_CHOICE, Difficulty.BEGINNER,
                "Java 基础", "A", "公共解析"
        ));
        Question ownQuestion = repository.save(new Question(
                null, USER.userId(), "我的个人题", QuestionType.SINGLE_CHOICE, Difficulty.BEGINNER,
                "Java 基础", "A", "个人解析"
        ));
        Question foreignQuestion = repository.save(new Question(
                null, OTHER_USER.userId(), "他人的个人题", QuestionType.SINGLE_CHOICE, Difficulty.BEGINNER,
                "Java 基础", "A", "他人解析"
        ));
        QuestionController controller = new QuestionController(repository);

        controller.deleteQuestion(USER, ownQuestion.id());
        controller.deleteQuestion(ADMIN, publicQuestion.id());

        assertThatThrownBy(() -> controller.deleteQuestion(USER, foreignQuestion.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("题目不存在或无权访问");
        assertThat(repository.search(null, null, null, null)).extracting(Question::id)
                .containsExactly(foreignQuestion.id());
    }
}
