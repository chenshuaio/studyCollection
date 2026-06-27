package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;

import java.util.List;

public class QuestionSearchService {
    private final List<Question> questions;

    public QuestionSearchService(List<Question> questions) {
        this.questions = questions;
    }

    public List<Question> search(String knowledgePoint, Difficulty difficulty, QuestionType type) {
        return questions.stream()
                .filter(question -> question.knowledgePoint().equals(knowledgePoint))
                .filter(question -> question.difficulty() == difficulty)
                .filter(question -> question.type() == type)
                .toList();
    }
}
