package com.studycollection.importer.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.importer.parser.ImportPreview;
import com.studycollection.importer.parser.MarkdownQuestionParser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/imports")
public class ImportPreviewController {
    private final MarkdownQuestionParser parser;

    public ImportPreviewController() {
        this(new MarkdownQuestionParser());
    }

    public ImportPreviewController(MarkdownQuestionParser parser) {
        this.parser = parser;
    }

    @PostMapping("/preview")
    public ApiResponse<ImportPreview> preview(@RequestBody ImportPreviewRequest request) {
        return ApiResponse.success(parser.parse(request.content()));
    }
}
