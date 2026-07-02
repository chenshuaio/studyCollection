package com.studycollection.importer.api;

import com.studycollection.importer.generator.GeneratedQuestionBank;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeGenerateControllerTest {
    @Test
    void generatesQuestionBankDraftsFromKnowledgeContent() {
        KnowledgeGenerateController controller = new KnowledgeGenerateController();

        GeneratedQuestionBank bank = controller.generate(new KnowledgeGenerateRequest("""
                HashMap 默认负载因子是 0.75。
                Java 支持封装、继承和多态。
                """)).data();

        assertThat(bank.questions()).isNotEmpty();
        assertThat(bank.questions().get(0).title()).contains("HashMap");
        assertThat(bank.questions().get(0).analysis()).isNotBlank();
    }

    @Test
    void generatesQuestionBankDraftsFromUploadedKnowledgeFile() throws Exception {
        KnowledgeGenerateController controller = new KnowledgeGenerateController();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hashmap.md",
                "text/markdown",
                """
                # HashMap 学习笔记
                HashMap 默认负载因子是 0.75，达到阈值后会扩容。
                Java 中局部变量没有默认值。
                """.getBytes(StandardCharsets.UTF_8)
        );

        GeneratedQuestionBank bank = controller.upload(file).data();

        assertThat(bank.questions()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(bank.questions())
                .extracting(question -> question.title())
                .anySatisfy(title -> assertThat(title).contains("HashMap"));
    }

    @Test
    void generatesQuestionBankDraftsFromUploadedDocxKnowledgeFile() throws Exception {
        KnowledgeGenerateController controller = new KnowledgeGenerateController();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hashmap.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes("""
                HashMap 默认负载因子是 0.75。
                Java 中局部变量没有默认值。
                """)
        );

        GeneratedQuestionBank bank = controller.upload(file).data();

        assertThat(bank.questions())
                .extracting(question -> question.title())
                .anySatisfy(title -> assertThat(title).contains("HashMap"));
    }

    @Test
    void generatesQuestionBankDraftsFromUploadedPdfKnowledgeFile() throws Exception {
        KnowledgeGenerateController controller = new KnowledgeGenerateController();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hashmap.pdf",
                "application/pdf",
                pdfBytes("HashMap default load factor is 0.75.")
        );

        GeneratedQuestionBank bank = controller.upload(file).data();

        assertThat(bank.questions())
                .extracting(question -> question.title())
                .anySatisfy(title -> assertThat(title).contains("HashMap"));
    }

    @Test
    void generatesQuestionBankDraftsFromUploadedXlsxKnowledgeFile() throws Exception {
        KnowledgeGenerateController controller = new KnowledgeGenerateController();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "java-notes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes("JVM 栈保存局部变量表，堆保存对象实例。")
        );

        GeneratedQuestionBank bank = controller.upload(file).data();

        assertThat(bank.questions())
                .extracting(question -> question.title())
                .anySatisfy(title -> assertThat(title).contains("JVM"));
    }

    private byte[] docxBytes(String text) throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(text);
            document.write(output);
            return output.toByteArray();
        }
    }

    private byte[] pdfBytes(String text) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(48, 720);
                content.showText(text);
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] xlsxBytes(String text) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Java");
            var row = sheet.createRow(0);
            row.createCell(0).setCellValue(text);
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
