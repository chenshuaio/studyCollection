package com.studycollection.exam.api;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;

import java.util.List;
import java.util.Map;

public record ExamRuleRequest(
        String name,
        String description,
        int durationMinutes,
        int totalQuestions,
        List<String> knowledgePoints,
        Map<QuestionType, Integer> typeQuotas,
        Map<Difficulty, Integer> difficultyQuotas
) {
}
