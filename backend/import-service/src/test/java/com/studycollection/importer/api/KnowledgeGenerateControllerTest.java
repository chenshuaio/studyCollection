package com.studycollection.importer.api;

import com.studycollection.importer.generator.GeneratedQuestionBank;
import org.junit.jupiter.api.Test;

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
}
