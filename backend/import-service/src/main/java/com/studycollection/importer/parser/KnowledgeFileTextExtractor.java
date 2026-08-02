package com.studycollection.importer.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class KnowledgeFileTextExtractor {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    public String extract(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("学习资料文件不能为空。");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("学习资料文件不能超过 10 MB。");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        byte[] bytes = file.getBytes();
        if (filename.endsWith(".docx")) {
            return extractDocx(bytes);
        }
        if (filename.endsWith(".pdf")) {
            return extractPdf(bytes);
        }
        if (filename.endsWith(".xlsx")) {
            return extractXlsx(bytes);
        }
        if (filename.endsWith(".txt") || filename.endsWith(".md") || filename.endsWith(".csv")) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("学习资料仅支持 PDF、DOCX、XLSX、CSV、MD 和 TXT 格式。");
    }

    private String extractDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    private String extractPdf(byte[] bytes) {
        try (PDDocument document = PDDocument.load(bytes)) {
            String text = new PDFTextStripper().getText(document).trim();
            if (text.isBlank()) {
                throw new IllegalArgumentException("PDF 未提取到可用文字，请上传可复制文字的 PDF，扫描版暂不支持。");
            }
            return text;
        } catch (IOException exception) {
            throw new IllegalArgumentException("PDF 无法解析，请确认文件未损坏且未加密。", exception);
        }
    }

    private String extractXlsx(byte[] bytes) throws IOException {
        DataFormatter formatter = new DataFormatter();
        List<String> lines = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            workbook.forEach(sheet -> sheet.forEach(row -> lines.add(formatRow(row, formatter))));
        }
        return lines.stream()
                .filter(line -> !line.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private String formatRow(Row row, DataFormatter formatter) {
        List<String> cells = new ArrayList<>();
        row.forEach(cell -> cells.add(formatter.formatCellValue(cell)));
        return String.join(" ", cells);
    }
}
