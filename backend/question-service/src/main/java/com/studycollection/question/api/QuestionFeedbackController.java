package com.studycollection.question.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.question.app.QuestionFeedbackService;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/questions/feedback")
public class QuestionFeedbackController {
    private final QuestionFeedbackService feedbackService;

    public QuestionFeedbackController() {
        this(new QuestionFeedbackService());
    }

    public QuestionFeedbackController(QuestionFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ApiResponse<QuestionFeedback> submit(@RequestBody SubmitFeedbackRequest request) {
        return ApiResponse.success(feedbackService.submit(
                request.userId(),
                request.questionId(),
                request.type(),
                request.content()
        ));
    }

    @GetMapping("/pending")
    public ApiResponse<List<QuestionFeedback>> pending() {
        return ApiResponse.success(feedbackService.pending());
    }

    @PostMapping("/{feedbackId}/accept")
    public ApiResponse<QuestionRevision> accept(
            @PathVariable("feedbackId") Long feedbackId,
            @RequestBody AcceptFeedbackRequest request
    ) {
        return ApiResponse.success(feedbackService.accept(
                feedbackId,
                request.adminUserId(),
                request.changeSummary(),
                request.reviewNote()
        ));
    }

    @PostMapping("/{feedbackId}/reject")
    public ApiResponse<QuestionFeedback> reject(
            @PathVariable("feedbackId") Long feedbackId,
            @RequestBody ReviewFeedbackRequest request
    ) {
        return ApiResponse.success(feedbackService.reject(
                feedbackId,
                request.adminUserId(),
                request.reviewNote()
        ));
    }

    @PostMapping("/{feedbackId}/needs-review")
    public ApiResponse<QuestionFeedback> markNeedsReview(
            @PathVariable("feedbackId") Long feedbackId,
            @RequestBody ReviewFeedbackRequest request
    ) {
        return ApiResponse.success(feedbackService.markNeedsReview(
                feedbackId,
                request.adminUserId(),
                request.reviewNote()
        ));
    }
}
