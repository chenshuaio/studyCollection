package com.studycollection.exam.api;

import java.util.List;

public record CustomExamRequest(String name, int durationMinutes, List<Long> questionIds) {
}
