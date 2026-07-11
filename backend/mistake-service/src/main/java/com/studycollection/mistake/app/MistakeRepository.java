package com.studycollection.mistake.app;

import com.studycollection.mistake.domain.MistakeRecord;

import java.util.List;

public interface MistakeRepository {
    MistakeRecord save(MistakeRecord record);

    MistakeRecord find(Long userId, Long questionId);

    List<MistakeRecord> findByUserId(Long userId);
}
