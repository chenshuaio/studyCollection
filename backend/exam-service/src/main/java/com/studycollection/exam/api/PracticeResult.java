package com.studycollection.exam.api;

import java.util.List;

public record PracticeResult(int score, int totalScore, List<PracticeResultItem> items) {
}
