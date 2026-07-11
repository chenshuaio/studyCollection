package com.studycollection.importer.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.importer.parser.ImportPreview;
import com.studycollection.importer.parser.StructuredQuestionFileParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/imports/questions")
public class StructuredQuestionImportController {
    private final StructuredQuestionFileParser parser;

    public StructuredQuestionImportController() {
        this(new StructuredQuestionFileParser());
    }

    @Autowired
    public StructuredQuestionImportController(StructuredQuestionFileParser parser) {
        this.parser = parser;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportPreview> upload(@RequestPart("file") MultipartFile file) throws IOException {
        return ApiResponse.success(parser.parse(file));
    }
}
