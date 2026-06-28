package com.studycollection.importer.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.importer.generator.GeneratedQuestionBank;
import com.studycollection.importer.generator.KnowledgeQuestionGenerator;
import com.studycollection.importer.parser.KnowledgeFileTextExtractor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/imports/knowledge")
public class KnowledgeGenerateController {
    private final KnowledgeQuestionGenerator generator;
    private final KnowledgeFileTextExtractor textExtractor;

    public KnowledgeGenerateController() {
        this(new KnowledgeQuestionGenerator(), new KnowledgeFileTextExtractor());
    }

    public KnowledgeGenerateController(KnowledgeQuestionGenerator generator) {
        this(generator, new KnowledgeFileTextExtractor());
    }

    public KnowledgeGenerateController(KnowledgeQuestionGenerator generator, KnowledgeFileTextExtractor textExtractor) {
        this.generator = generator;
        this.textExtractor = textExtractor;
    }

    @PostMapping("/generate")
    public ApiResponse<GeneratedQuestionBank> generate(@RequestBody KnowledgeGenerateRequest request) {
        return ApiResponse.success(generator.generate(request.content()));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<GeneratedQuestionBank> upload(@RequestPart("file") MultipartFile file) throws IOException {
        return ApiResponse.success(generator.generate(textExtractor.extract(file)));
    }
}
