package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryQuestionRepository implements QuestionRepository {
    private final AtomicLong ids = new AtomicLong(1);
    private final List<Question> questions = new ArrayList<>();

    @Override
    public Question save(Question question) {
        Question saved = new Question(
                question.id() == null ? ids.getAndIncrement() : question.id(),
                question.title(),
                question.type(),
                question.difficulty(),
                question.knowledgePoint(),
                question.answer(),
                question.analysis()
        );
        questions.add(saved);
        return saved;
    }

    @Override
    public List<Question> search(String knowledgePoint, Difficulty difficulty, QuestionType type) {
        return questions.stream()
                .filter(question -> question.knowledgePoint().equals(knowledgePoint))
                .filter(question -> question.difficulty() == difficulty)
                .filter(question -> question.type() == type)
                .toList();
    }
}
