package com.studycollection.importer.api;

import com.studycollection.importer.generator.GeneratedQuestionBank;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeGenerateControllerTest {
    @Test
    void generatesQuestionBankDraftsFromKnowledgeContent() {
        KnowledgeGenerateController controller = new KnowledgeGenerateController();

        GeneratedQuestionBank bank = controller.generate(new KnowledgeGenerateRequest("""
                HashMap 默认负载因子是 0.75。
                Java 支持封装、继承和多态。
                """)).data();

        assertThat(bank.questions()).isNotEmpty();
        assertThat(bank.questions().get(0).title()).contains("HashMap");
        assertThat(bank.questions().get(0).analysis()).isNotBlank();
    }

    @Test
    void generatesQuestionBankDraftsFromUploadedKnowledgeFile() throws Exception {
        KnowledgeGenerateController controller = new KnowledgeGenerateController();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hashmap.md",
                "text/markdown",
                """
                # HashMap 学习笔记
                HashMap 默认负载因子是 0.75，达到阈值后会扩容。
                Java 中局部变量没有默认值。
                """.getBytes()
        );

        GeneratedQuestionBank bank = controller.upload(file).data();

        assertThat(bank.questions()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(bank.questions())
                .extracting(question -> question.title())
                .anySatisfy(title -> assertThat(title).contains("HashMap"));
    }
}
