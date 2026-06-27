package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("!local-mysql")
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
