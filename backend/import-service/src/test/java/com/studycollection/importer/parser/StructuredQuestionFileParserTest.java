package com.studycollection.importer.parser;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StructuredQuestionFileParserTest {
    private final StructuredQuestionFileParser parser = new StructuredQuestionFileParser();

    @Test
    void parsesJsonArrayAndQuestionObjectWithoutLosingOptions() throws Exception {
        ImportPreview arrayPreview = parser.parse(file("questions.json", """
                [
                  {
                    "title": "下列哪个是 Java 关键字？",
                    "type": "SINGLE_CHOICE",
                    "difficulty": "BEGINNER",
                    "knowledgePoint": "Java 基础",
                    "answer": "A",
                    "analysis": "class 是关键字",
                    "optionA": "class",
                    "optionB": "hello"
                  }
                ]
                """));
        ImportPreview objectPreview = parser.parse(file("questions.json", """
                {"questions":[{
                  "题目":"JVM 是什么？",
                  "题型":"简答题",
                  "难度":"进阶",
                  "知识点":"JVM",
                  "答案":"Java Virtual Machine",
                  "解析":"Java 虚拟机"
                }]}
                """));

        assertThat(arrayPreview.questions()).singleElement().satisfies(question -> {
            assertThat(question.title()).contains("A. class", "B. hello");
            assertThat(question.type()).isEqualTo("SINGLE_CHOICE");
        });
        assertThat(objectPreview.questions()).singleElement().satisfies(question -> {
            assertThat(question.type()).isEqualTo("SHORT_ANSWER");
            assertThat(question.difficulty()).isEqualTo("INTERMEDIATE");
        });
    }

    @Test
    void parsesQuotedCsvRowsAndOptionColumns() throws Exception {
        ImportPreview preview = parser.parse(file("questions.csv", """
                title,type,difficulty,knowledgePoint,answer,analysis,optionA,optionB
                "HashMap 的键和值可以为 null 吗？",SINGLE_CHOICE,INTERMEDIATE,集合框架,A,"键和值规则不同，需要区分",允许,不允许
                "说明 List, Set 的区别",SHORT_ANSWER,ADVANCED,集合框架,"List 有序可重复，Set 不重复","比较顺序与重复元素",,
                """));

        assertThat(preview.questions()).hasSize(2);
        assertThat(preview.questions().get(0).title()).contains("A. 允许", "B. 不允许");
        assertThat(preview.questions().get(1).answer()).contains("List 有序可重复，Set 不重复");
    }

    @Test
    void parsesXlsxRowsByHeaderAndSkipsEmptyRows() throws Exception {
        ImportPreview preview = parser.parse(new MockMultipartFile(
                "file",
                "questions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes()
        ));

        assertThat(preview.questions()).hasSize(2);
        assertThat(preview.questions()).extracting(ParsedQuestion::knowledgePoint)
                .containsExactly("Java 基础", "并发编程");
        assertThat(preview.questions().get(1).title()).contains("A. synchronized", "B. volatile");
    }

    @Test
    void rejectsUnsupportedFilesAndReportsInvalidRow() {
        assertThatThrownBy(() -> parser.parse(file("questions.pdf", "not a question table")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("仅支持");
        assertThatThrownBy(() -> parser.parse(file("questions.csv", """
                title,type,difficulty,knowledgePoint,answer
                缺少答案,SINGLE_CHOICE,BEGINNER,Java 基础,
                """)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("第 1 题")
                .hasMessageContaining("答案");
    }

    private MockMultipartFile file(String filename, String content) {
        return new MockMultipartFile("file", filename, "application/octet-stream", content.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] xlsxBytes() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("题库");
            var header = sheet.createRow(0);
            String[] headers = {"题目", "题型", "难度", "知识点", "答案", "解析", "选项A", "选项B"};
            for (int index = 0; index < headers.length; index++) {
                header.createCell(index).setCellValue(headers[index]);
            }
            var first = sheet.createRow(1);
            String[] firstValues = {"Java 源文件扩展名是什么？", "FILL_BLANK", "BEGINNER", "Java 基础", ".java", "源文件扩展名", "", ""};
            for (int index = 0; index < firstValues.length; index++) {
                first.createCell(index).setCellValue(firstValues[index]);
            }
            sheet.createRow(2);
            var third = sheet.createRow(3);
            String[] thirdValues = {"哪个关键字可用于同步？", "单选题", "进阶", "并发编程", "A", "synchronized 用于同步", "synchronized", "volatile"};
            for (int index = 0; index < thirdValues.length; index++) {
                third.createCell(index).setCellValue(thirdValues[index]);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
