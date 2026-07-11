package com.studycollection.mistake.api;

import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.common.security.Role;
import com.studycollection.mistake.app.InMemoryMistakeRepository;
import com.studycollection.mistake.app.MistakeService;
import com.studycollection.mistake.domain.MistakeRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MistakeControllerTest {
    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "alice", Role.USER);

    @Test
    void recordsAndListsUserMistakes() {
        MistakeController controller = controller();

        MistakeRecord record = controller.record(USER, new RecordMistakeRequest(
                1L,
                "HashMap 默认负载因子是多少？",
                "集合框架",
                "PENDING"
        )).data();
        List<MistakeRecord> mistakes = controller.list(USER).data();

        assertThat(record.questionTitle()).contains("HashMap");
        assertThat(mistakes).hasSize(1);
        assertThat(mistakes.get(0).knowledgePoint()).isEqualTo("集合框架");
    }

    @Test
    void updatesMistakeMasteryStatus() {
        MistakeController controller = controller();
        controller.record(USER, new RecordMistakeRequest(
                1L,
                "HashMap 默认负载因子是多少？",
                "集合框架",
                "PENDING"
        ));

        MistakeRecord updated = controller.updateStatus(USER, new UpdateMistakeStatusRequest(
                1L,
                "MASTERED"
        )).data();
        List<MistakeRecord> mistakes = controller.list(USER).data();

        assertThat(updated.status()).isEqualTo("MASTERED");
        assertThat(mistakes).hasSize(1);
        assertThat(mistakes.get(0).status()).isEqualTo("MASTERED");
    }

    private MistakeController controller() {
        return new MistakeController(new MistakeService(new InMemoryMistakeRepository()));
    }
}
