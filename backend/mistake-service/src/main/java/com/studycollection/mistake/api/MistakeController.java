package com.studycollection.mistake.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.mistake.app.MistakeService;
import com.studycollection.mistake.domain.MistakeRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/mistakes")
public class MistakeController {
    private final MistakeService mistakeService;

    public MistakeController() {
        this(new MistakeService());
    }

    public MistakeController(MistakeService mistakeService) {
        this.mistakeService = mistakeService;
    }

    @PostMapping
    public ApiResponse<MistakeRecord> record(@RequestBody RecordMistakeRequest request) {
        return ApiResponse.success(mistakeService.record(
                request.userId(),
                request.questionId(),
                request.questionTitle(),
                request.knowledgePoint(),
                request.status()
        ));
    }

    @GetMapping
    public ApiResponse<List<MistakeRecord>> list(@RequestParam("userId") Long userId) {
        return ApiResponse.success(mistakeService.list(userId));
    }

    @PostMapping("/status")
    public ApiResponse<MistakeRecord> updateStatus(@RequestBody UpdateMistakeStatusRequest request) {
        return ApiResponse.success(mistakeService.updateStatus(
                request.userId(),
                request.questionId(),
                request.status()
        ));
    }
}
