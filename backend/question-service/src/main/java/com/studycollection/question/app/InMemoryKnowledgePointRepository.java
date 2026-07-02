package com.studycollection.question.app;

import com.studycollection.question.domain.KnowledgePoint;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("!local-mysql")
public class InMemoryKnowledgePointRepository implements KnowledgePointRepository {
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, KnowledgePoint> repository = new ConcurrentHashMap<>();

    @Override
    public KnowledgePoint save(KnowledgePoint knowledgePoint) {
        Long id = knowledgePoint.id() == null ? idGenerator.getAndIncrement() : knowledgePoint.id();
        KnowledgePoint saved = new KnowledgePoint(
                id,
                knowledgePoint.name(),
                knowledgePoint.description(),
                knowledgePoint.enabled()
        );
        repository.put(id, saved);
        return saved;
    }

    @Override
    public List<KnowledgePoint> findAll() {
        return repository.values().stream()
                .sorted(Comparator.comparing(KnowledgePoint::id))
                .toList();
    }

    @Override
    public KnowledgePoint disable(Long id) {
        KnowledgePoint current = repository.get(id);
        if (current == null) {
            throw new IllegalArgumentException("知识点不存在");
        }
        return save(new KnowledgePoint(current.id(), current.name(), current.description(), false));
    }
}
