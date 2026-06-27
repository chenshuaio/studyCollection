package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionSearchServiceTest {
    @Test
    void filtersByKnowledgePointDifficultyAndType() {
        QuestionSearchService service = new QuestionSearchService(List.of(
                new Question(1L, "ArrayList 扩容机制", QuestionType.SINGLE_CHOICE, Difficulty.BEGINNER, "集合框架"),
                new Question(2L, "JVM GC Roots", QuestionType.SHORT_ANSWER, Difficulty.ADVANCED, "JVM")
        ));

        List<Question> result = service.search("集合框架", Difficulty.BEGINNER, QuestionType.SINGLE_CHOICE);

        assertThat(result).extracting(Question::id).containsExactly(1L);
    }
}
