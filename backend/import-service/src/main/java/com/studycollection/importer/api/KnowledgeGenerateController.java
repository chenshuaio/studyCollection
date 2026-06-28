package com.studycollection.importer.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.importer.generator.GeneratedQuestionBank;
import com.studycollection.importer.generator.KnowledgeQuestionGenerator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<GeneratedQuestionBank> upload(@RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("学习资料文件不能为空");
        }
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return ApiResponse.success(generator.generate(content));
    }
}
