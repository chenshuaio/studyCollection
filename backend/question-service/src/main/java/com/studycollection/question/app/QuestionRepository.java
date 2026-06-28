package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;

import java.util.List;

public interface QuestionRepository {
    Question save(Question question);

    List<Question> search(String keyword, String knowledgePoint, Difficulty difficulty, QuestionType type);
}
