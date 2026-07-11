package com.studycollection.importer.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class StructuredQuestionFileParser {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_QUESTION_COUNT = 1000;

    private final ObjectMapper objectMapper;
    private final MarkdownQuestionParser markdownParser;
    private final QuestionDraftFactory draftFactory;

    public StructuredQuestionFileParser() {
        this(new ObjectMapper(), new MarkdownQuestionParser(), new QuestionDraftFactory());
    }

    StructuredQuestionFileParser(
            ObjectMapper objectMapper,
            MarkdownQuestionParser markdownParser,
            QuestionDraftFactory draftFactory
    ) {
        this.objectMapper = objectMapper;
        this.markdownParser = markdownParser;
        this.draftFactory = draftFactory;
    }

    public ImportPreview parse(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("题目文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("题目文件不能超过 10 MB");
        }
        String filename = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        ImportPreview preview;
        if (filename.endsWith(".json")) {
            preview = parseJson(file.getBytes());
        } else if (filename.endsWith(".csv")) {
            preview = parseCsv(file.getBytes());
        } else if (filename.endsWith(".xlsx")) {
            preview = parseXlsx(file.getBytes());
        } else if (filename.endsWith(".txt") || filename.endsWith(".md")) {
            preview = markdownParser.parse(stripBom(new String(file.getBytes(), StandardCharsets.UTF_8)));
        } else {
            throw new IllegalArgumentException("结构化题目文件仅支持 JSON、CSV、XLSX、TXT 和 MD 格式");
        }
        if (preview.questions().isEmpty()) {
            throw new IllegalArgumentException("题目文件中没有可导入的题目");
        }
        if (preview.questions().size() > MAX_QUESTION_COUNT) {
            throw new IllegalArgumentException("单次最多导入 1000 道题目");
        }
        return preview;
    }

    private ImportPreview parseJson(byte[] bytes) throws IOException {
        JsonNode root;
        try {
            root = objectMapper.readTree(stripBom(new String(bytes, StandardCharsets.UTF_8)));
        } catch (IOException exception) {
            throw new IllegalArgumentException("JSON 解析失败，请检查语法：" + exception.getMessage(), exception);
        }
        JsonNode questionsNode = root != null && root.isArray() ? root : root == null ? null : root.get("questions");
        if (questionsNode == null || !questionsNode.isArray()) {
            throw new IllegalArgumentException("JSON 根节点必须是题目数组，或包含 questions 数组");
        }
        List<ParsedQuestion> questions = new ArrayList<>();
        for (JsonNode node : questionsNode) {
            if (!node.isObject()) {
                throw new IllegalArgumentException("第 " + (questions.size() + 1) + " 题必须是 JSON 对象");
            }
            Map<String, String> fields = new LinkedHashMap<>();
            node.fields().forEachRemaining(entry -> fields.put(entry.getKey(), entry.getValue().asText("")));
            questions.add(draftFactory.create(questions.size() + 1, fields));
        }
        return new ImportPreview(List.copyOf(questions));
    }

    private ImportPreview parseCsv(byte[] bytes) throws IOException {
        String content = stripBom(new String(bytes, StandardCharsets.UTF_8));
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setIgnoreSurroundingSpaces(true)
                .setTrim(true)
                .build();
        List<ParsedQuestion> questions = new ArrayList<>();
        try (var parser = format.parse(new StringReader(content))) {
            for (CSVRecord record : parser) {
                Map<String, String> fields = record.toMap();
                if (fields.values().stream().allMatch(String::isBlank)) {
                    continue;
                }
                questions.add(draftFactory.create(questions.size() + 1, fields));
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("CSV 解析失败，请检查表头和引号：" + exception.getMessage(), exception);
        }
        return new ImportPreview(List.copyOf(questions));
    }

    private ImportPreview parseXlsx(byte[] bytes) throws IOException {
        List<ParsedQuestion> questions = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            workbook.forEach(sheet -> parseSheet(sheet.rowIterator(), formatter, questions));
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("XLSX 解析失败，请检查工作簿格式：" + exception.getMessage(), exception);
        }
        return new ImportPreview(List.copyOf(questions));
    }

    private void parseSheet(Iterator<Row> rows, DataFormatter formatter, List<ParsedQuestion> questions) {
        if (!rows.hasNext()) {
            return;
        }
        Row headerRow = rows.next();
        Map<Integer, String> headers = new LinkedHashMap<>();
        for (int index = 0; index < headerRow.getLastCellNum(); index++) {
            String header = formatter.formatCellValue(headerRow.getCell(index)).trim();
            if (!header.isBlank()) {
                headers.put(index, header);
            }
        }
        if (headers.isEmpty()) {
            throw new IllegalArgumentException("XLSX 首行必须包含题目字段表头");
        }
        while (rows.hasNext()) {
            Row row = rows.next();
            Map<String, String> fields = new LinkedHashMap<>();
            headers.forEach((index, header) -> fields.put(header, formatter.formatCellValue(row.getCell(index)).trim()));
            if (fields.values().stream().allMatch(String::isBlank)) {
                continue;
            }
            questions.add(draftFactory.create(questions.size() + 1, fields));
        }
    }

    private String stripBom(String content) {
        return content.startsWith("\uFEFF") ? content.substring(1) : content;
    }
}
