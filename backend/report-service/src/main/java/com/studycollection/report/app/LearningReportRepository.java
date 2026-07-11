package com.studycollection.report.app;

import java.util.List;

public interface LearningReportRepository {
    LearningReportResponse save(Long userId, LearningReportResponse report);

    List<LearningReportResponse> findByUserId(Long userId);
}
