package com.studycollection.importer.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(preview.questions().get(0).title()).isEqualTo("Java 中 int 默认值是多少？");
        assertThat(preview.questions().get(0).answer()).isEqualTo("A");
        assertThat(preview.questions().get(0).knowledgePoint()).isEqualTo("Java 基础");
        assertThat(preview.questions().get(0).difficulty()).isEqualTo("BEGINNER");
    }
}
