package com.studycollection.importer.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Collectors;

public class KnowledgeFileTextExtractor {
    public String extract(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("学习资料文件不能为空");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        byte[] bytes = file.getBytes();
        if (filename.endsWith(".docx")) {
            return extractDocx(bytes);
        }
        if (filename.endsWith(".pdf")) {
            return extractPdf(bytes);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String extractDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    private String extractPdf(byte[] bytes) throws IOException {
        try (PDDocument document = PDDocument.load(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
