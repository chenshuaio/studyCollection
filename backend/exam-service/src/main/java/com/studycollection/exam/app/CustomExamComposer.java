package com.studycollection.exam.app;

import com.studycollection.exam.domain.ExamPaper;

import java.util.List;

public class CustomExamComposer {
    public ExamPaper compose(String name, int durationMinutes, List<Long> selectedQuestionIds) {
        if (selectedQuestionIds.isEmpty()) {
            throw new IllegalArgumentException("至少选择一道题目");
        }
        return new ExamPaper(name, durationMinutes, List.copyOf(selectedQuestionIds));
    }
}
