package com.studycollection.exam.domain;

import java.util.List;

public record ExamPaper(String name, int durationMinutes, List<Long> questionIds) {
}
