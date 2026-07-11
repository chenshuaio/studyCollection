package com.studycollection.exam.app;

import java.util.List;

public interface LearningAttemptRepository {
    void saveAll(List<LearningAttempt> attempts);

    List<LearningAttempt> findByUserId(Long userId);
}
