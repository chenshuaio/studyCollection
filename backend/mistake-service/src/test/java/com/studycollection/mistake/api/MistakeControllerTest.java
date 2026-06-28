package com.studycollection.mistake.api;

import com.studycollection.mistake.domain.MistakeRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MistakeControllerTest {
    @Test
    void recordsAndListsUserMistakes() {
        MistakeController controller = new MistakeController();

        MistakeRecord record = controller.record(new RecordMistakeRequest(
                7L,
                1L,
                "HashMap 默认负载因子是多少？",
                "集合框架",
                "PENDING"
        )).data();
        List<MistakeRecord> mistakes = controller.list(7L).data();

        assertThat(record.questionTitle()).contains("HashMap");
        assertThat(mistakes).hasSize(1);
        assertThat(mistakes.get(0).knowledgePoint()).isEqualTo("集合框架");
    }
}
