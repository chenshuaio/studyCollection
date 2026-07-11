package com.studycollection.report.app;

import java.time.Instant;
import java.util.List;

public record LearningReportResponse(
        Long id,
        Instant createdAt,
        String weakestKnowledgePoint,
        String recommendation,
        String adviceSource,
        String adviceContent,
        int answeredQuestionCount,
        int gradedQuestionCount,
        int correctQuestionCount,
        double accuracy,
        List<PerformanceBreakdown> knowledgePointPerformance,
        List<PerformanceBreakdown> questionTypePerformance,
        List<TrendPoint> recentTrend,
        List<StrengtheningQuestion> strengtheningQuestions
) {
    public LearningReportResponse {
        knowledgePointPerformance = List.copyOf(knowledgePointPerformance);
        questionTypePerformance = List.copyOf(questionTypePerformance);
        recentTrend = List.copyOf(recentTrend);
        strengtheningQuestions = List.copyOf(strengtheningQuestions);
    }

    public LearningReportResponse withIdentity(Long persistedId, Instant persistedAt) {
        return new LearningReportResponse(
                persistedId,
                persistedAt,
                weakestKnowledgePoint,
                recommendation,
                adviceSource,
                adviceContent,
                answeredQuestionCount,
                gradedQuestionCount,
                correctQuestionCount,
                accuracy,
                knowledgePointPerformance,
                questionTypePerformance,
                recentTrend,
                strengtheningQuestions
        );
    }
}
