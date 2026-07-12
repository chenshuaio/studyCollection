package com.studycollection.mistake.app;

import com.studycollection.mistake.domain.MistakeRecord;
import com.studycollection.mistake.domain.MistakeFilter;

import java.util.List;

public interface MistakeRepository {
    MistakeRecord recordOccurrence(MistakeRecord record);

    MistakeRecord updateStatus(Long userId, Long questionId, String status);

    MistakeRecord find(Long userId, Long questionId);

    List<MistakeRecord> findByUserId(Long userId, MistakeFilter filter);
}
