package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.FeedbackType;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionType;

import java.time.Instant;
import java.util.List;

public record QuestionFeedbackGroup(
        Long questionId,
        String questionTitle,
        QuestionType questionType,
        Difficulty difficulty,
        String knowledgePoint,
        String currentAnswer,
        String currentAnalysis,
        String questionSource,
        FeedbackType type,
        int feedbackCount,
        Instant latestAt,
        List<QuestionFeedback> items
) {
    public QuestionFeedbackGroup {
        items = List.copyOf(items);
    }
}
