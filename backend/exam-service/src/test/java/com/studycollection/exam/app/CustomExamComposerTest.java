package com.studycollection.exam.app;

import com.studycollection.exam.domain.ExamPaper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomExamComposerTest {
    @Test
    void createsUserPaperFromSelectedQuestionIds() {
        CustomExamComposer composer = new CustomExamComposer();

        ExamPaper paper = composer.compose("集合专项测试", 45, List.of(11L, 12L, 13L));

        assertThat(paper.name()).isEqualTo("集合专项测试");
        assertThat(paper.durationMinutes()).isEqualTo(45);
        assertThat(paper.questionIds()).containsExactly(11L, 12L, 13L);
    }
}
