package com.studycollection.report.app;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("!local-mysql")
public class InMemoryLearningReportRepository implements LearningReportRepository {
    private final AtomicLong ids = new AtomicLong(1);
    private final List<StoredReport> reports = new ArrayList<>();

    @Override
    public synchronized LearningReportResponse save(Long userId, LearningReportResponse report) {
        LearningReportResponse saved = report.withIdentity(ids.getAndIncrement(), report.createdAt());
        reports.add(new StoredReport(userId, saved));
        return saved;
    }

    @Override
    public synchronized List<LearningReportResponse> findByUserId(Long userId) {
        return reports.stream()
                .filter(stored -> stored.userId().equals(userId))
                .map(StoredReport::report)
                .sorted(Comparator.comparing(LearningReportResponse::createdAt)
                        .thenComparing(LearningReportResponse::id)
                        .reversed())
                .toList();
    }

    private record StoredReport(Long userId, LearningReportResponse report) {
    }
}
