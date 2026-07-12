package com.studycollection.mistake.app;

import com.studycollection.mistake.domain.MistakeFilter;
import com.studycollection.mistake.domain.MistakeRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@Profile("!local-mysql")
public class InMemoryMistakeRepository implements MistakeRepository {
    private final Map<String, MistakeRecord> records = new LinkedHashMap<>();

    @Override
    public synchronized MistakeRecord recordOccurrence(MistakeRecord occurrence) {
        String key = key(occurrence.userId(), occurrence.questionId());
        MistakeRecord existing = records.get(key);
        MistakeRecord saved = existing == null ? occurrence : new MistakeRecord(
                existing.userId(),
                existing.questionId(),
                occurrence.questionTitle(),
                occurrence.questionType(),
                occurrence.knowledgePoint(),
                occurrence.lastSubmittedAnswer(),
                occurrence.sourceContext(),
                "PENDING",
                existing.wrongCount() + 1,
                existing.firstWrongAt(),
                occurrence.lastWrongAt()
        );
        records.put(key, saved);
        return saved;
    }

    @Override
    public synchronized MistakeRecord updateStatus(Long userId, Long questionId, String status) {
        MistakeRecord existing = find(userId, questionId);
        MistakeRecord updated = new MistakeRecord(
                existing.userId(),
                existing.questionId(),
                existing.questionTitle(),
                existing.questionType(),
                existing.knowledgePoint(),
                existing.lastSubmittedAnswer(),
                existing.sourceContext(),
                status,
                existing.wrongCount(),
                existing.firstWrongAt(),
                existing.lastWrongAt()
        );
        records.put(key(userId, questionId), updated);
        return updated;
    }

    @Override
    public synchronized MistakeRecord find(Long userId, Long questionId) {
        MistakeRecord record = records.get(key(userId, questionId));
        if (record == null) {
            throw new IllegalArgumentException("错题记录不存在");
        }
        return record;
    }

    @Override
    public synchronized List<MistakeRecord> findByUserId(Long userId, MistakeFilter filter) {
        return records.values().stream()
                .filter(record -> record.userId().equals(userId))
                .filter(filter::matches)
                .sorted(Comparator.comparing(MistakeRecord::lastWrongAt).reversed()
                        .thenComparing(Comparator.comparing(MistakeRecord::questionId).reversed()))
                .toList();
    }

    private String key(Long userId, Long questionId) {
        return userId + "|" + questionId;
    }
}
