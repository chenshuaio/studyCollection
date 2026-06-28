package com.studycollection.mistake.app;

import com.studycollection.mistake.domain.MistakeRecord;

import java.util.ArrayList;
import java.util.List;

public class MistakeService {
    private final List<MistakeRecord> records = new ArrayList<>();

    public MistakeRecord record(Long userId, Long questionId, String questionTitle, String knowledgePoint, String status) {
        MistakeRecord record = new MistakeRecord(userId, questionId, questionTitle, knowledgePoint, status);
        records.removeIf(existing -> existing.userId().equals(userId) && existing.questionId().equals(questionId));
        records.add(record);
        return record;
    }

    public List<MistakeRecord> list(Long userId) {
        return records.stream()
                .filter(record -> record.userId().equals(userId))
                .toList();
    }
}
