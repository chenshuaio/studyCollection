package com.studycollection.question.app;

import com.studycollection.question.domain.PendingQuestion;
import com.studycollection.question.domain.PendingQuestionStatus;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryPendingQuestionRepository implements PendingQuestionRepository {
    private final AtomicLong ids = new AtomicLong(1);
    private final Map<Long, PendingQuestion> questions = new LinkedHashMap<>();

    @Override
    public PendingQuestion save(PendingQuestion question) {
        Long id = question.id() == null ? ids.getAndIncrement() : question.id();
        PendingQuestion saved = new PendingQuestion(
                id,
                question.submitterUserId(),
                question.title(),
                question.type(),
                question.difficulty(),
                question.knowledgePoint(),
                question.answer(),
                question.analysis(),
                question.status()
        );
        questions.put(id, saved);
        return saved;
    }

    @Override
    public PendingQuestion find(Long id) {
        PendingQuestion question = questions.get(id);
        if (question == null) {
            throw new IllegalArgumentException("待审核题目不存在");
        }
        return question;
    }

    @Override
    public List<PendingQuestion> findByStatus(PendingQuestionStatus status) {
        return questions.values().stream()
                .filter(question -> question.status() == status)
                .toList();
    }
}
