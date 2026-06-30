package com.studycollection.importer.generator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeQuestionGeneratorTest {
    @Test
    void generatesQuestionDraftsFromJavaLearningContent() {
        String content = """
                HashMap 是 Java 集合框架中的常用 Map 实现。
                HashMap 默认负载因子是 0.75，达到阈值后会进行扩容。
                Java 中局部变量没有默认值，必须先赋值再使用。
                """;

        GeneratedQuestionBank bank = new KnowledgeQuestionGenerator().generate(content);

        assertThat(bank.questions()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(bank.questions())
                .anySatisfy(question -> {
                    assertThat(question.title()).contains("HashMap 默认负载因子");
                    assertThat(question.type()).isEqualTo("SINGLE_CHOICE");
                    assertThat(List.of("A", "B", "C", "D")).contains(question.answer());
                    assertThat(question.title()).contains("A.", "B.", "C.", "D.");
                    assertThat(optionText(question.title(), question.answer())).contains("0.75");
                    assertThat(question.analysis()).contains("0.75");
                    assertThat(question.knowledgePoint()).isEqualTo("集合框架");
                })
                .anySatisfy(question -> {
                    assertThat(question.title()).contains("局部变量");
                    assertThat(question.answer()).isEqualTo("错误");
                    assertThat(question.analysis()).contains("必须先赋值");
                });
    }

    @Test
    void generatedSingleChoiceAnswersAreNotAlwaysA() {
        KnowledgeQuestionGenerator generator = new KnowledgeQuestionGenerator();

        List<String> answers = IntStream.range(0, 12)
                .mapToObj(index -> generator.generate("HashMap 默认负载因子是 0.75。材料编号 " + index))
                .flatMap(bank -> bank.questions().stream())
                .filter(question -> question.title().contains("HashMap 默认负载因子"))
                .map(GeneratedQuestion::answer)
                .distinct()
                .toList();

        assertThat(answers).hasSizeGreaterThan(1);
        assertThat(answers).isNotEqualTo(List.of("A"));
    }

    private String optionText(String title, String answer) {
        return title.lines()
                .filter(line -> line.startsWith(answer + "."))
                .findFirst()
                .orElse("");
    }
}
