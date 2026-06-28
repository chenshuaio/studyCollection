package com.studycollection.exam.api;

import java.util.List;

public record PracticeSubmitRequest(List<PracticeAnswer> answers, Long userId) {
    public PracticeSubmitRequest(List<PracticeAnswer> answers) {
        this(answers, null);
    }
}
