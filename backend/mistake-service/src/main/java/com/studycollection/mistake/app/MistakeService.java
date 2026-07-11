package com.studycollection.mistake.app;

import com.studycollection.mistake.domain.MistakeRecord;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MistakeService {
    private final MistakeRepository mistakeRepository;

    public MistakeService(MistakeRepository mistakeRepository) {
        this.mistakeRepository = mistakeRepository;
    }

    public MistakeRecord record(Long userId, Long questionId, String questionTitle, String knowledgePoint, String status) {
        MistakeRecord record = new MistakeRecord(userId, questionId, questionTitle, knowledgePoint, status);
        return mistakeRepository.save(record);
    }

    public MistakeRecord updateStatus(Long userId, Long questionId, String status) {
        MistakeRecord existing = mistakeRepository.find(userId, questionId);
        return record(
                existing.userId(),
                existing.questionId(),
                existing.questionTitle(),
                existing.knowledgePoint(),
                status
        );
    }

    public List<MistakeRecord> list(Long userId) {
        return mistakeRepository.findByUserId(userId);
    }
}
