package com.studycollection.exam.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.exam.app.ExamSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.List;

@RestController
@RequestMapping("/exams")
public class CustomExamController {
    private final ExamSessionService service;
    private final Clock clock;

    @Autowired
    public CustomExamController(ExamSessionService service) {
        this(service, Clock.systemUTC());
    }

    public CustomExamController(ExamSessionService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @PostMapping("/custom")
    public ApiResponse<ExamSessionResponse> create(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestBody CustomExamRequest request
    ) {
        return response(service.create(currentUser.userId(), request));
    }

    @GetMapping
    public ApiResponse<List<ExamSummaryResponse>> list(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(service.list(currentUser.userId()).stream()
                .map(ExamSummaryResponse::from)
                .toList());
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<ExamSessionResponse> get(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable("sessionId") Long sessionId
    ) {
        return response(service.get(currentUser.userId(), sessionId));
    }

    @PutMapping("/{sessionId}/answers/{questionId}")
    public ApiResponse<ExamSessionResponse> saveAnswer(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable("sessionId") Long sessionId,
            @PathVariable("questionId") Long questionId,
            @RequestBody SaveExamAnswerRequest request
    ) {
        String answer = request == null ? "" : request.answer();
        return response(service.saveAnswer(currentUser.userId(), sessionId, questionId, answer));
    }

    @PostMapping("/{sessionId}/submit")
    public ApiResponse<ExamSessionResponse> submit(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable("sessionId") Long sessionId
    ) {
        return response(service.submit(currentUser.userId(), sessionId));
    }

    private ApiResponse<ExamSessionResponse> response(com.studycollection.exam.domain.ExamSession session) {
        return ApiResponse.success(ExamSessionResponse.from(session, clock.instant()));
    }
}
