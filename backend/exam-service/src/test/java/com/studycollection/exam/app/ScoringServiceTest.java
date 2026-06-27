package com.studycollection.exam.app;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringServiceTest {
    @Test
    void scoresObjectiveAnswers() {
        ScoringService service = new ScoringService();

        int score = service.score(
                Map.of(1L, "A", 2L, "true"),
                Map.of(1L, "A", 2L, "false"),
                5
        );

        assertThat(score).isEqualTo(5);
    }
}
