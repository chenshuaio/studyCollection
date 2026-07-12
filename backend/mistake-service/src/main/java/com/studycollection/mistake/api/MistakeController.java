package com.studycollection.mistake.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.mistake.app.MistakeService;
import com.studycollection.mistake.domain.MistakeRecord;
import com.studycollection.question.domain.QuestionType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/mistakes")
public class MistakeController {
    private final MistakeService mistakeService;

    public MistakeController(MistakeService mistakeService) {
        this.mistakeService = mistakeService;
    }

    @PostMapping
    public ApiResponse<MistakeRecord> record(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestBody RecordMistakeRequest request
    ) {
        return ApiResponse.success(mistakeService.record(
                currentUser.userId(),
                request.questionId(),
                request.submittedAnswer(),
                request.sourceContext()
        ));
    }

    @GetMapping
    public ApiResponse<List<MistakeRecord>> list(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestParam(value = "knowledgePoint", required = false) String knowledgePoint,
            @RequestParam(value = "questionType", required = false) QuestionType questionType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "wrongFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate wrongFrom,
            @RequestParam(value = "wrongTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate wrongTo
    ) {
        return ApiResponse.success(mistakeService.list(
                currentUser.userId(),
                knowledgePoint,
                questionType,
                status,
                wrongFrom,
                wrongTo
        ));
    }

    @PostMapping("/status")
    public ApiResponse<MistakeRecord> updateStatus(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestBody UpdateMistakeStatusRequest request
    ) {
        return ApiResponse.success(mistakeService.updateStatus(
                currentUser.userId(),
                request.questionId(),
                request.status()
        ));
    }
}
