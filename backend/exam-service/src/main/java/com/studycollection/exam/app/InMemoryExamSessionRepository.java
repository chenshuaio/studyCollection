package com.studycollection.exam.app;

import com.studycollection.exam.domain.ExamSession;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("!local-mysql")
public class InMemoryExamSessionRepository implements ExamSessionRepository {
    private final AtomicLong sequence = new AtomicLong();
    private final Map<Long, ExamSession> sessions = new ConcurrentHashMap<>();

    @Override
    public ExamSession create(ExamSession session) {
        ExamSession persisted = session.withId(sequence.incrementAndGet());
        sessions.put(persisted.id(), persisted);
        return persisted;
    }

    @Override
    public ExamSession save(ExamSession session) {
        if (session.id() == null || !sessions.containsKey(session.id())) {
            throw new IllegalArgumentException("考试记录不存在");
        }
        sessions.put(session.id(), session);
        return session;
    }

    @Override
    public ExamSession findById(Long id) {
        ExamSession session = sessions.get(id);
        if (session == null) {
            throw new IllegalArgumentException("考试记录不存在");
        }
        return session;
    }

    @Override
    public List<ExamSession> findByUserId(Long userId) {
        return sessions.values().stream()
                .filter(session -> session.userId().equals(userId))
                .sorted(Comparator.comparing(ExamSession::startedAt).reversed()
                        .thenComparing(ExamSession::id, Comparator.reverseOrder()))
                .toList();
    }
}
