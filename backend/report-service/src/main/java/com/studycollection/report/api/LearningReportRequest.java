package com.studycollection.report.api;

import java.util.List;

public record LearningReportRequest(String mode, List<ReportResultRequest> results) {
}
