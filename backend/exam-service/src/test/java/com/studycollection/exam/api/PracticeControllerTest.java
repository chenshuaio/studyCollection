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

    @Test
    void tracksSubmittedQuestionCountForUserDashboard() {
        PracticeController controller = new PracticeController();

        controller.submit(new PracticeSubmitRequest(List.of(
                new PracticeAnswer(1L, "A"),
                new PracticeAnswer(2L, "true")
        ), 7L));
        controller.submit(new PracticeSubmitRequest(List.of(
                new PracticeAnswer(3L, "B")
        ), 7L));

        PracticeStats stats = controller.stats(7L).data();

        assertThat(stats.userId()).isEqualTo(7L);
        assertThat(stats.answeredQuestionCount()).isEqualTo(3);
        assertThat(stats.correctQuestionCount()).isEqualTo(2);
    }

    @Test
    void scoresQuestionBankSnapshotAnswers() {
        PracticeController controller = new PracticeController();

        PracticeResult result = controller.submit(new PracticeSubmitRequest(List.of(
                new PracticeAnswer(99L, "0.75", "0.75", "HashMap 默认负载因子是 0.75。")
        ), 7L)).data();

        assertThat(result.score()).isEqualTo(10);
        assertThat(result.totalScore()).isEqualTo(10);
        assertThat(result.items().get(0).questionId()).isEqualTo(99L);
        assertThat(result.items().get(0).correct()).isTrue();
        assertThat(result.items().get(0).analysis()).contains("HashMap");
    }
}
