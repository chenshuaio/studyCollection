package com.studycollection.importer.api;

import com.studycollection.importer.parser.ImportPreview;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImportPreviewControllerTest {
    @Test
    void previewsMarkdownQuestionsFromRequestBody() {
        ImportPreviewController controller = new ImportPreviewController();

        ImportPreview preview = controller.preview(new ImportPreviewRequest("""
                ## 单选题
                题目: Java 中 HashMap 默认负载因子是多少？
                答案: 0.75
                知识点: 集合框架
                难度: INTERMEDIATE
                """)).data();

        assertThat(preview.questions()).hasSize(1);
        assertThat(preview.questions().get(0).title()).contains("HashMap");
        assertThat(preview.questions().get(0).knowledgePoint()).isEqualTo("集合框架");
    }
}
