package com.studycollection.exam.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.exam.app.CustomExamComposer;
import com.studycollection.exam.domain.ExamPaper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exams/custom")
public class CustomExamController {
    private final CustomExamComposer composer;

    public CustomExamController() {
        this(new CustomExamComposer());
    }

    public CustomExamController(CustomExamComposer composer) {
        this.composer = composer;
    }

    @PostMapping
    public ApiResponse<ExamPaper> create(@RequestBody CustomExamRequest request) {
        return ApiResponse.success(composer.compose(
                request.name(),
                request.durationMinutes(),
                request.questionIds()
        ));
    }
}
