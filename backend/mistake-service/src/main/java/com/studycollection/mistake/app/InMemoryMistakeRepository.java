package com.studycollection.mistake.app;

import com.studycollection.mistake.domain.MistakeRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@Profile("!local-mysql")
public class InMemoryMistakeRepository implements MistakeRepository {
    private final List<MistakeRecord> records = new ArrayList<>();

    @Override
    public synchronized MistakeRecord save(MistakeRecord record) {
        records.removeIf(existing -> existing.userId().equals(record.userId())
                && existing.questionId().equals(record.questionId()));
        records.add(record);
        return record;
    }

    @Override
    public synchronized MistakeRecord find(Long userId, Long questionId) {
        return records.stream()
                .filter(record -> record.userId().equals(userId) && record.questionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("错题记录不存在"));
    }

    @Override
    public synchronized List<MistakeRecord> findByUserId(Long userId) {
        return records.stream()
                .filter(record -> record.userId().equals(userId))
                .toList();
    }
}
