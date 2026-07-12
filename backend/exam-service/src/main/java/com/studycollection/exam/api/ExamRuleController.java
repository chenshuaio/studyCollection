package com.studycollection.exam.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.common.security.AdminOnly;
import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.exam.app.ExamRuleService;
import com.studycollection.exam.domain.ExamRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.List;

@RestController
@RequestMapping("/exam-rules")
public class ExamRuleController {
    private final ExamRuleService service;
    private final Clock clock;

    @Autowired
    public ExamRuleController(ExamRuleService service) {
        this(service, Clock.systemUTC());
    }

    public ExamRuleController(ExamRuleService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    public ApiResponse<List<ExamRule>> listPublished() {
        return ApiResponse.success(service.listPublished());
    }

    @GetMapping("/admin")
    @AdminOnly
    public ApiResponse<List<ExamRule>> listAll() {
        return ApiResponse.success(service.listAll());
    }

    @PostMapping
    @AdminOnly
    public ApiResponse<ExamRule> create(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestBody ExamRuleRequest request
    ) {
        return ApiResponse.success(service.create(currentUser.userId(), request));
    }

    @PutMapping("/{id}")
    @AdminOnly
    public ApiResponse<ExamRule> update(
            @PathVariable("id") Long id,
            @RequestBody ExamRuleRequest request
    ) {
        return ApiResponse.success(service.update(id, request));
    }

    @PostMapping("/{id}/publish")
    @AdminOnly
    public ApiResponse<ExamRule> publish(@PathVariable("id") Long id) {
        return ApiResponse.success(service.publish(id));
    }

    @PostMapping("/{id}/unpublish")
    @AdminOnly
    public ApiResponse<ExamRule> unpublish(@PathVariable("id") Long id) {
        return ApiResponse.success(service.unpublish(id));
    }

    @DeleteMapping("/{id}")
    @AdminOnly
    public ApiResponse<Long> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ApiResponse.success(id);
    }

    @PostMapping("/{id}/start")
    public ApiResponse<ExamSessionResponse> start(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable("id") Long id
    ) {
        return ApiResponse.success(ExamSessionResponse.from(service.start(currentUser.userId(), id), clock.instant()));
    }
}
