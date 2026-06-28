package com.studycollection.exam.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PracticeControllerTest {
    @Test
    void scoresSubmittedPracticeAndReturnsExplanations() {
        PracticeController controller = new PracticeController();

        PracticeResult result = controller.submit(new PracticeSubmitRequest(List.of(
                new PracticeAnswer(1L, "A"),
                new PracticeAnswer(2L, "true"),
                new PracticeAnswer(3L, "B")
        ))).data();

        assertThat(result.score()).isEqualTo(20);
        assertThat(result.totalScore()).isEqualTo(30);
        assertThat(result.items()).hasSize(3);
        assertThat(result.items().get(0).correct()).isTrue();
        assertThat(result.items().get(0).analysis()).contains("HashMap");
        assertThat(result.items().get(1).correct()).isTrue();
        assertThat(result.items().get(1).analysis()).contains("局部变量");
        assertThat(result.items().get(2).correct()).isFalse();
        assertThat(result.items().get(2).analysis()).contains("ArrayList");
    }
}
