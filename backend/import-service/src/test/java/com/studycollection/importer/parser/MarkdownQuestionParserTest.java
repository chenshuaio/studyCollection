package com.studycollection.importer.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownQuestionParserTest {
    @Test
    void parsesSimpleQuestionBlocksIntoPreview() {
        String markdown = """
                ## 单选题
                题目: Java 中 int 默认值是多少？
                A. 0
                B. null
                答案: A
                知识点: Java 基础
                难度: BEGINNER
                """;

        ImportPreview preview = new MarkdownQuestionParser().parse(markdown);

        assertThat(preview.questions()).hasSize(1);
        assertThat(preview.questions().get(0).title()).contains("Java 中 int 默认值是多少？", "A. 0", "B. null");
        assertThat(preview.questions().get(0).answer()).isEqualTo("A");
        assertThat(preview.questions().get(0).knowledgePoint()).isEqualTo("Java 基础");
        assertThat(preview.questions().get(0).difficulty()).isEqualTo("BEGINNER");
        assertThat(preview.questions().get(0).type()).isEqualTo("SINGLE_CHOICE");
    }

    @Test
    void keepsChoiceOptionLinesWithQuestionTitle() {
        String markdown = """
                题目: Java 中 int 默认值是多少？
                A. 0
                B. null
                C. 1
                D. 不确定
                答案: A
                知识点: Java 基础
                难度: BEGINNER
                """;

        ImportPreview preview = new MarkdownQuestionParser().parse(markdown);

        assertThat(preview.questions()).hasSize(1);
        assertThat(preview.questions().get(0).title()).contains("Java 中 int 默认值是多少？", "A. 0", "D. 不确定");
        assertThat(preview.questions().get(0).answer()).isEqualTo("A");
    }

    @Test
    void parsesMultipleQuestionBlocksWithCompleteEditableFields() {
        ImportPreview preview = new MarkdownQuestionParser().parse("""
                ## 单选题
                题目: HashMap 默认负载因子是多少？
                A. 0.5
                B. 0.75
                答案: B
                解析: 默认负载因子为 0.75。
                知识点: 集合框架
                难度: 进阶

                ## 简答题
                题目: 简述 JVM 堆的用途。
                答案: 保存对象实例和数组。
                解析: 堆是线程共享的运行时数据区。
                知识点: JVM
                难度: ADVANCED
                """);

        assertThat(preview.questions()).hasSize(2);
        assertThat(preview.questions().get(0)).satisfies(question -> {
            assertThat(question.type()).isEqualTo("SINGLE_CHOICE");
            assertThat(question.difficulty()).isEqualTo("INTERMEDIATE");
            assertThat(question.title()).contains("A. 0.5", "B. 0.75");
            assertThat(question.analysis()).contains("0.75");
        });
        assertThat(preview.questions().get(1)).satisfies(question -> {
            assertThat(question.type()).isEqualTo("SHORT_ANSWER");
            assertThat(question.knowledgePoint()).isEqualTo("JVM");
            assertThat(question.analysis()).contains("线程共享");
        });
    }

    @Test
    void reportsQuestionNumberAndMissingField() {
        assertThatThrownBy(() -> new MarkdownQuestionParser().parse("""
                ## 填空题
                题目: Java 源文件扩展名是什么？
                知识点: Java 基础
                难度: BEGINNER
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("第 1 题")
                .hasMessageContaining("答案");
    }
}
