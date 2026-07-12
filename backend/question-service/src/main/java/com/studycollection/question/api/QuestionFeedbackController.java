package com.studycollection.question.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.common.security.AdminOnly;
import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.question.app.QuestionFeedbackService;
import com.studycollection.question.app.QuestionFeedbackGroup;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/questions/feedback")
public class QuestionFeedbackController {
    private final QuestionFeedbackService feedbackService;

    public QuestionFeedbackController(QuestionFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ApiResponse<QuestionFeedback> submit(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestBody SubmitFeedbackRequest request
    ) {
        return ApiResponse.success(feedbackService.submit(
                currentUser.userId(),
                request.questionId(),
                request.type(),
                request.content(),
                request.submittedAnswer(),
                request.sourceContext(),
                request.sourceReference()
        ));
    }

    @GetMapping("/pending")
    @AdminOnly
    public ApiResponse<List<QuestionFeedback>> pending() {
        return ApiResponse.success(feedbackService.pending());
    }

    @GetMapping("/pending/groups")
    @AdminOnly
    public ApiResponse<List<QuestionFeedbackGroup>> pendingGroups() {
        return ApiResponse.success(feedbackService.pendingGroups());
    }

    @GetMapping
    public ApiResponse<List<QuestionFeedback>> byUser(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(feedbackService.byUser(currentUser.userId()));
    }

    @PostMapping("/{feedbackId}/accept")
    @AdminOnly
    public ApiResponse<QuestionRevision> accept(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable("feedbackId") Long feedbackId,
            @RequestBody AcceptFeedbackRequest request
    ) {
        return ApiResponse.success(feedbackService.accept(
                feedbackId,
                currentUser.userId(),
                request.changeSummary(),
                request.reviewNote(),
                request.correctedTitle(),
                request.correctedType(),
                request.correctedDifficulty(),
                request.correctedKnowledgePoint(),
                request.correctedAnswer(),
                request.correctedAnalysis()
        ));
    }

    @PostMapping("/groups/accept")
    @AdminOnly
    public ApiResponse<QuestionRevision> acceptGroup(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestBody AcceptFeedbackGroupRequest request
    ) {
        return ApiResponse.success(feedbackService.acceptGroup(
                request.feedbackIds(),
                currentUser.userId(),
                request.changeSummary(),
                request.reviewNote(),
                request.correctedTitle(),
                request.correctedType(),
                request.correctedDifficulty(),
                request.correctedKnowledgePoint(),
                request.correctedAnswer(),
                request.correctedAnalysis()
        ));
    }

    @GetMapping("/revisions/{questionId}")
    @AdminOnly
    public ApiResponse<List<QuestionRevision>> revisions(
            @PathVariable("questionId") Long questionId
    ) {
        return ApiResponse.success(feedbackService.revisions(questionId));
    }

    @PostMapping("/{feedbackId}/reject")
    @AdminOnly
    public ApiResponse<QuestionFeedback> reject(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable("feedbackId") Long feedbackId,
            @RequestBody ReviewFeedbackRequest request
    ) {
        return ApiResponse.success(feedbackService.reject(
                feedbackId,
                currentUser.userId(),
                request.reviewNote()
        ));
    }

    @PostMapping("/{feedbackId}/needs-review")
    @AdminOnly
    public ApiResponse<QuestionFeedback> markNeedsReview(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable("feedbackId") Long feedbackId,
            @RequestBody ReviewFeedbackRequest request
    ) {
        return ApiResponse.success(feedbackService.markNeedsReview(
                feedbackId,
                currentUser.userId(),
                request.reviewNote()
        ));
    }
}
