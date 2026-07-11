package com.studycollection.exam.app;

import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PracticeGeneratorTest {
    private PracticeGenerator generator;

    @BeforeEach
    void setUp() {
        InMemoryQuestionRepository repository = new InMemoryQuestionRepository();
        repository.save(question(1L, "List 是接口吗？", QuestionType.TRUE_FALSE, Difficulty.BEGINNER, "集合框架"));
        repository.save(question(2L, "Set 是否允许重复元素？", QuestionType.TRUE_FALSE, Difficulty.BEGINNER, "集合框架"));
        repository.save(question(3L, "HashMap 默认负载因子？", QuestionType.FILL_BLANK, Difficulty.INTERMEDIATE, "集合框架"));
        repository.save(question(4L, "局部变量有默认值吗？", QuestionType.TRUE_FALSE, Difficulty.BEGINNER, "Java 基础"));
        generator = new PracticeGenerator(repository, new Random(7));
    }

    @Test
    void filtersByKnowledgeDifficultyAndTypeThenLimitsWithoutDuplicates() {
        List<Question> generated = generator.generate(
                "集合框架",
                Difficulty.BEGINNER,
                QuestionType.TRUE_FALSE,
                2
        );

        assertThat(generated).hasSize(2);
        assertThat(generated).extracting(Question::id).doesNotHaveDuplicates();
        assertThat(generated).allSatisfy(question -> {
            assertThat(question.knowledgePoint()).isEqualTo("集合框架");
            assertThat(question.difficulty()).isEqualTo(Difficulty.BEGINNER);
            assertThat(question.type()).isEqualTo(QuestionType.TRUE_FALSE);
        });
    }

    @Test
    void returnsAllAvailableQuestionsWhenRequestedCountIsLarger() {
        List<Question> generated = generator.generate(
                "集合框架",
                null,
                null,
                10
        );

        assertThat(generated).hasSize(3);
        assertThat(generated).extracting(Question::id).doesNotHaveDuplicates();
    }

    @Test
    void rejectsInvalidCountAndEmptyMatches() {
        assertThatThrownBy(() -> generator.generate(null, null, null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("练习题目数量必须在 1 到 100 之间");
        assertThatThrownBy(() -> generator.generate(null, null, null, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("练习题目数量必须在 1 到 100 之间");
        assertThatThrownBy(() -> generator.generate("不存在的知识点", null, null, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("没有符合条件的可用题目");
    }

    private Question question(
            Long id,
            String title,
            QuestionType type,
            Difficulty difficulty,
            String knowledgePoint
    ) {
        return new Question(id, title, type, difficulty, knowledgePoint, "A", "题目解析");
    }
}
