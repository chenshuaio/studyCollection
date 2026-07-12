package com.studycollection.exam.app;

import com.studycollection.exam.domain.ExamRule;
import com.studycollection.exam.domain.ExamRuleStatus;
import com.studycollection.question.app.InMemoryQuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RuleBasedExamGeneratorTest {
    private static final Instant NOW = Instant.parse("2026-07-12T02:00:00Z");

    @Test
    void validatesQuestionAndDifficultyQuotaTotals() {
        assertThatIllegalArgumentException().isThrownBy(() -> rule(
                3,
                Map.of(QuestionType.SINGLE_CHOICE, 2),
                Map.of(Difficulty.BEGINNER, 3),
                List.of()
        )).withMessageContaining("题型配额");

        assertThatIllegalArgumentException().isThrownBy(() -> rule(
                3,
                Map.of(QuestionType.SINGLE_CHOICE, 3),
                Map.of(Difficulty.BEGINNER, 2),
                List.of()
        )).withMessageContaining("难度配额");

        ExamRule allKnowledgePoints = rule(
                2,
                Map.of(QuestionType.SINGLE_CHOICE, 2),
                Map.of(Difficulty.BEGINNER, 2),
                List.of()
        );

        assertThat(allKnowledgePoints.knowledgePoints()).isEmpty();
    }

    @Test
    void rejectsQuotaTotalsThatOverflowIntegerRange() {
        assertThatIllegalArgumentException().isThrownBy(() -> rule(
                1,
                Map.of(
                        QuestionType.SINGLE_CHOICE, Integer.MAX_VALUE,
                        QuestionType.MULTIPLE_CHOICE, Integer.MAX_VALUE,
                        QuestionType.TRUE_FALSE, 3
                ),
                Map.of(Difficulty.BEGINNER, 1),
                List.of()
        )).withMessageContaining("题型配额");
    }

    @Test
    void validatesRuleFieldsAndRejectsEmptyQuotaKeys() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ExamRule(
                1L,
                " ",
                "",
                60,
                1,
                List.of(),
                Map.of(QuestionType.SINGLE_CHOICE, 1),
                Map.of(Difficulty.BEGINNER, 1),
                ExamRuleStatus.DRAFT,
                1L,
                NOW,
                NOW
        )).withMessageContaining("考试名称");

        assertThatIllegalArgumentException().isThrownBy(() -> new ExamRule(
                1L,
                "Java 模拟考试",
                "",
                0,
                1,
                List.of(),
                Map.of(QuestionType.SINGLE_CHOICE, 1),
                Map.of(Difficulty.BEGINNER, 1),
                ExamRuleStatus.DRAFT,
                1L,
                NOW,
                NOW
        )).withMessageContaining("考试时长");

        Map<QuestionType, Integer> quotasWithNullKey = new HashMap<>();
        quotasWithNullKey.put(QuestionType.SINGLE_CHOICE, 1);
        quotasWithNullKey.put(null, 1);

        assertThatIllegalArgumentException().isThrownBy(() -> rule(
                1,
                quotasWithNullKey,
                Map.of(Difficulty.BEGINNER, 1),
                List.of()
        )).withMessageContaining("题型配额不能包含空键");

        assertThatIllegalArgumentException().isThrownBy(() -> new ExamRule(
                1L,
                "测".repeat(129),
                "",
                60,
                1,
                List.of(),
                Map.of(QuestionType.SINGLE_CHOICE, 1),
                Map.of(Difficulty.BEGINNER, 1),
                ExamRuleStatus.DRAFT,
                1L,
                NOW,
                NOW
        )).withMessageContaining("128");
    }

    @Test
    void generatesQuestionsMatchingKnowledgeTypeAndDifficultyQuotas() {
        InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
        save(questions, "单选-入门-1", QuestionType.SINGLE_CHOICE, Difficulty.BEGINNER, "Java 基础");
        save(questions, "单选-入门-2", QuestionType.SINGLE_CHOICE, Difficulty.BEGINNER, "Java 基础");
        save(questions, "单选-进阶-1", QuestionType.SINGLE_CHOICE, Difficulty.INTERMEDIATE, "Java 基础");
        save(questions, "多选-入门-1", QuestionType.MULTIPLE_CHOICE, Difficulty.BEGINNER, "Java 基础");
        save(questions, "多选-进阶-1", QuestionType.MULTIPLE_CHOICE, Difficulty.INTERMEDIATE, "Java 基础");
        save(questions, "多选-进阶-2", QuestionType.MULTIPLE_CHOICE, Difficulty.INTERMEDIATE, "Java 基础");
        save(questions, "范围外题目", QuestionType.SINGLE_CHOICE, Difficulty.BEGINNER, "集合框架");

        ExamRule rule = rule(
                4,
                Map.of(QuestionType.SINGLE_CHOICE, 2, QuestionType.MULTIPLE_CHOICE, 2),
                Map.of(Difficulty.BEGINNER, 2, Difficulty.INTERMEDIATE, 2),
                List.of("Java 基础")
        );

        List<Question> generated = new RuleBasedExamGenerator(questions).generate(rule);

        assertThat(generated).hasSize(4);
        assertThat(generated).extracting(Question::knowledgePoint).containsOnly("Java 基础");
        assertThat(generated).extracting(Question::type)
                .containsExactlyInAnyOrder(
                        QuestionType.SINGLE_CHOICE,
                        QuestionType.SINGLE_CHOICE,
                        QuestionType.MULTIPLE_CHOICE,
                        QuestionType.MULTIPLE_CHOICE
                );
        assertThat(generated).extracting(Question::difficulty)
                .containsExactlyInAnyOrder(
                        Difficulty.BEGINNER,
                        Difficulty.BEGINNER,
                        Difficulty.INTERMEDIATE,
                        Difficulty.INTERMEDIATE
                );
    }

    @Test
    void rejectsRuleWhenJointDistributionCannotBeSatisfied() {
        InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
        save(questions, "单选入门", QuestionType.SINGLE_CHOICE, Difficulty.BEGINNER, "Java 基础");
        save(questions, "多选入门", QuestionType.MULTIPLE_CHOICE, Difficulty.BEGINNER, "Java 基础");
        save(questions, "判断进阶", QuestionType.TRUE_FALSE, Difficulty.INTERMEDIATE, "Java 基础");

        ExamRule rule = rule(
                2,
                Map.of(QuestionType.SINGLE_CHOICE, 1, QuestionType.MULTIPLE_CHOICE, 1),
                Map.of(Difficulty.BEGINNER, 1, Difficulty.INTERMEDIATE, 1),
                List.of("Java 基础")
        );

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RuleBasedExamGenerator(questions).generate(rule))
                .withMessageContaining("题库不足")
                .withMessageContaining("题型与难度");
    }

    @Test
    void publicSimulationNeverUsesPersonalQuestionCapacity() {
        InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
        save(questions, "公共单选题", QuestionType.SINGLE_CHOICE, Difficulty.BEGINNER, "Java 基础");
        questions.save(new Question(
                null,
                7L,
                "个人单选题",
                QuestionType.SINGLE_CHOICE,
                Difficulty.BEGINNER,
                "Java 基础",
                "A",
                "个人解析"
        ));
        ExamRule rule = rule(
                2,
                Map.of(QuestionType.SINGLE_CHOICE, 2),
                Map.of(Difficulty.BEGINNER, 2),
                List.of("Java 基础")
        );

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RuleBasedExamGenerator(questions).generate(rule))
                .withMessageContaining("题库不足");
    }

    private ExamRule rule(
            int totalQuestions,
            Map<QuestionType, Integer> typeQuotas,
            Map<Difficulty, Integer> difficultyQuotas,
            List<String> knowledgePoints
    ) {
        return new ExamRule(
                1L,
                "Java 模拟考试",
                "验证 Java 核心知识",
                60,
                totalQuestions,
                knowledgePoints,
                typeQuotas,
                difficultyQuotas,
                ExamRuleStatus.DRAFT,
                1L,
                NOW,
                NOW
        );
    }

    private void save(
            InMemoryQuestionRepository repository,
            String title,
            QuestionType type,
            Difficulty difficulty,
            String knowledgePoint
    ) {
        repository.save(new Question(null, title, type, difficulty, knowledgePoint, "A", "解析"));
    }
}
