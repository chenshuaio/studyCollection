package com.studycollection.question.api;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;

import java.util.List;

public record AcceptFeedbackGroupRequest(
        List<Long> feedbackIds,
        String changeSummary,
        String reviewNote,
        String correctedTitle,
        QuestionType correctedType,
        Difficulty correctedDifficulty,
        String correctedKnowledgePoint,
        String correctedAnswer,
        String correctedAnalysis
) {
}
