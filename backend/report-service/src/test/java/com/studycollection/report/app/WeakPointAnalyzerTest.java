package com.studycollection.report.app;

import com.studycollection.exam.app.LearningActivityType;
import com.studycollection.exam.app.LearningAttempt;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeakPointAnalyzerTest {
    @Test
    void identifiesLowestObjectiveAccuracyWithoutTreatingSubjectiveAnswersAsWrong() {
        WeakPointAnalyzer analyzer = new WeakPointAnalyzer();

        LearningReport report = analyzer.analyze(List.of(
                attempt("集合框架", true, true),
                attempt("集合框架", true, false),
                attempt("JVM", true, false),
                attempt("JVM", false, false)
        ));

        assertThat(report.weakestKnowledgePoint()).isEqualTo("JVM");
        assertThat(report.recommendation()).contains("JVM", "0%");
    }

    private LearningAttempt attempt(String knowledgePoint, boolean autoGraded, boolean correct) {
        return new LearningAttempt(
                null,
                7L,
                LearningActivityType.PRACTICE,
                knowledgePoint + autoGraded + correct,
                (long) (knowledgePoint + autoGraded + correct).hashCode(),
                "测试题",
                autoGraded ? QuestionType.FILL_BLANK : QuestionType.SHORT_ANSWER,
                Difficulty.INTERMEDIATE,
                knowledgePoint,
                "答案",
                autoGraded,
                autoGraded ? correct : null,
                correct ? 10 : 0,
                Instant.parse("2026-07-11T08:00:00Z")
        );
    }
}
