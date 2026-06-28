package com.studycollection.report.api;

import com.studycollection.ai.app.AiAnalysisService;
import com.studycollection.ai.app.AnalysisAdvice;
import com.studycollection.ai.app.AnalysisMode;
import com.studycollection.common.api.ApiResponse;
import com.studycollection.report.app.LearningReport;
import com.studycollection.report.app.LearningReportResponse;
import com.studycollection.report.app.WeakPointAnalyzer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports/learning")
public class LearningReportController {
    private final WeakPointAnalyzer analyzer;
    private final AiAnalysisService aiAnalysisService;

    public LearningReportController() {
        this(new WeakPointAnalyzer(), new AiAnalysisService());
    }

    public LearningReportController(WeakPointAnalyzer analyzer, AiAnalysisService aiAnalysisService) {
        this.analyzer = analyzer;
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping
    public ApiResponse<LearningReportResponse> generate(@RequestBody LearningReportRequest request) {
        LearningReport report = analyzer.analyze(request.results().stream()
                .map(result -> new WeakPointAnalyzer.Result(result.knowledgePoint(), result.correct()))
                .toList());
        AnalysisMode mode = AnalysisMode.valueOf(request.mode());
        AnalysisAdvice advice = aiAnalysisService.generate(mode, report.recommendation());
        return ApiResponse.success(new LearningReportResponse(
                report.weakestKnowledgePoint(),
                report.recommendation(),
                advice.source(),
                advice.content()
        ));
    }
}
