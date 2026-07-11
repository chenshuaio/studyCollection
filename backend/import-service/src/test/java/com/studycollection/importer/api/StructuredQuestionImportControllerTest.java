package com.studycollection.importer.api;

import com.studycollection.importer.parser.ImportPreview;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredQuestionImportControllerTest {
    @Test
    void uploadsStructuredQuestionFileForPreview() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questions.json",
                "application/json",
                """
                [{
                  "title":"Java 的入口方法是什么？",
                  "type":"SHORT_ANSWER",
                  "difficulty":"BEGINNER",
                  "knowledgePoint":"Java 基础",
                  "answer":"main 方法",
                  "analysis":"public static void main"
                }]
                """.getBytes(StandardCharsets.UTF_8)
        );

        ImportPreview preview = new StructuredQuestionImportController().upload(file).data();

        assertThat(preview.questions()).singleElement().satisfies(question -> {
            assertThat(question.title()).contains("入口方法");
            assertThat(question.type()).isEqualTo("SHORT_ANSWER");
            assertThat(question.analysis()).contains("public static void main");
        });
    }
}
