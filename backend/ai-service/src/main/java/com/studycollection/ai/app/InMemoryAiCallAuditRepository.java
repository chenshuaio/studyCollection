package com.studycollection.ai.app;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("!local-mysql")
public class InMemoryAiCallAuditRepository implements AiCallAuditRepository {
    private final AtomicLong ids = new AtomicLong(1);
    private final List<AiCallAudit> audits = new ArrayList<>();

    @Override
    public synchronized AiCallAudit save(AiCallAudit audit) {
        AiCallAudit saved = new AiCallAudit(
                audit.id() == null ? ids.getAndIncrement() : audit.id(),
                audit.userId(),
                audit.purpose(),
                audit.provider(),
                audit.modelName(),
                audit.status(),
                audit.failureReason(),
                audit.durationMs(),
                audit.createdAt()
        );
        audits.add(saved);
        return saved;
    }

    @Override
    public synchronized List<AiCallAudit> findRecent(int limit) {
        return audits.stream()
                .sorted(Comparator.comparing(AiCallAudit::createdAt)
                        .thenComparing(AiCallAudit::id)
                        .reversed())
                .limit(limit)
                .toList();
    }
}
