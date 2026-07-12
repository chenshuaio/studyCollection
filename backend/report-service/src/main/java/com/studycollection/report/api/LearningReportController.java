package com.studycollection.report.api;

import com.studycollection.ai.app.AnalysisMode;
import com.studycollection.common.api.ApiResponse;
import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.report.app.LearningReportResponse;
import com.studycollection.report.app.LearningReportService;
import com.studycollection.report.app.RevisedQuestionPolicy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reports/learning")
public class LearningReportController {
    private final LearningReportService service;

    public LearningReportController(LearningReportService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<LearningReportResponse> generate(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestBody LearningReportRequest request
    ) {
        return ApiResponse.success(service.generate(
                currentUser.userId(),
                parseMode(request.mode()),
                parseRevisionPolicy(request.revisedQuestionPolicy())
        ));
    }

    @GetMapping
    public ApiResponse<List<LearningReportResponse>> history(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(service.history(currentUser.userId()));
    }

    private AnalysisMode parseMode(String value) {
        try {
            return AnalysisMode.valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("分析模式仅支持 OFFLINE_RULES 或 ONLINE_MODEL");
        }
    }

    private RevisedQuestionPolicy parseRevisionPolicy(String value) {
        String normalized = value == null || value.isBlank() ? "EXCLUDE_REVISED" : value.trim().toUpperCase();
        try {
            return RevisedQuestionPolicy.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("修订题策略仅支持 EXCLUDE_REVISED 或 RECALCULATE_REVISED");
        }
    }
}
