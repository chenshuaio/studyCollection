package com.studycollection.exam.api;

import com.studycollection.exam.domain.ExamPaper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomExamControllerTest {
    @Test
    void createsCustomPaperFromSelectedQuestions() {
        CustomExamController controller = new CustomExamController();

        ExamPaper paper = controller.create(new CustomExamRequest(
                "集合专项测试",
                45,
                List.of(1L, 2L, 3L)
        )).data();

        assertThat(paper.name()).isEqualTo("集合专项测试");
        assertThat(paper.durationMinutes()).isEqualTo(45);
        assertThat(paper.questionIds()).containsExactly(1L, 2L, 3L);
    }
}
