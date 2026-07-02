package com.studycollection.question.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.question.app.KnowledgePointRepository;
import com.studycollection.question.domain.KnowledgePoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/knowledge-points")
public class KnowledgePointController {
    private final KnowledgePointRepository knowledgePointRepository;

    public KnowledgePointController(KnowledgePointRepository knowledgePointRepository) {
        this.knowledgePointRepository = knowledgePointRepository;
    }

    @GetMapping
    public ApiResponse<List<KnowledgePoint>> list() {
        return ApiResponse.success(knowledgePointRepository.findAll());
    }

    @PostMapping
    public ApiResponse<KnowledgePoint> create(@RequestBody CreateKnowledgePointRequest request) {
        return ApiResponse.success(knowledgePointRepository.save(new KnowledgePoint(
                null,
                request.name(),
                request.description(),
                true
        )));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<KnowledgePoint> disable(@PathVariable("id") Long id) {
        return ApiResponse.success(knowledgePointRepository.disable(id));
    }
}
