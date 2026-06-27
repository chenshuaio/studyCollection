package com.studycollection.importer.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.importer.generator.GeneratedQuestionBank;
import com.studycollection.importer.generator.KnowledgeQuestionGenerator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/imports/knowledge")
public class KnowledgeGenerateController {
    private final KnowledgeQuestionGenerator generator;

    public KnowledgeGenerateController() {
        this(new KnowledgeQuestionGenerator());
    }

    public KnowledgeGenerateController(KnowledgeQuestionGenerator generator) {
        this.generator = generator;
    }

    @PostMapping("/generate")
    public ApiResponse<GeneratedQuestionBank> generate(@RequestBody KnowledgeGenerateRequest request) {
        return ApiResponse.success(generator.generate(request.content()));
    }
}
