package com.studycollection.importer.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeFileTextExtractorTest {
    private static final String EMPTY_FILE_MESSAGE = "学习资料文件不能为空。";
    private static final String OVERSIZED_FILE_MESSAGE = "学习资料文件不能超过 10 MB。";
    private static final String UNSUPPORTED_FILE_MESSAGE = "学习资料仅支持 PDF、DOCX、XLSX、CSV、MD 和 TXT 格式。";
    private static final String BLANK_PDF_MESSAGE = "PDF 未提取到可用文字，请上传可复制文字的 PDF，扫描版暂不支持。";
    private static final String INVALID_PDF_MESSAGE = "PDF 无法解析，请确认文件未损坏且未加密。";

    private final KnowledgeFileTextExtractor extractor = new KnowledgeFileTextExtractor();

    @Test
    void extractsTextFromPdfUsingFilenameExtensionInsteadOfMimeType() throws Exception {
        MockMultipartFile file = file("hashmap.PDF", "text/plain", textPdfBytes("HashMap uses buckets."));

        String text = extractor.extract(file);

        assertThat(text).contains("HashMap");
    }

    @Test
    void rejectsBlankPdf() throws Exception {
        assertThatThrownBy(() -> extractor.extract(file("blank.pdf", "application/pdf", blankPdfBytes())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(BLANK_PDF_MESSAGE);
    }

    @Test
    void rejectsBrokenPdf() {
        assertThatThrownBy(() -> extractor.extract(file(
                "broken.pdf",
                "application/pdf",
                "not a pdf".getBytes(StandardCharsets.UTF_8)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(INVALID_PDF_MESSAGE);
    }

    @Test
    void rejectsPasswordProtectedPdf() throws Exception {
        assertThatThrownBy(() -> extractor.extract(file(
                "protected.pdf",
                "application/pdf",
                encryptedPdfBytes("user-password")
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(INVALID_PDF_MESSAGE);
    }

    @Test
    void rejectsOwnerPasswordProtectedPdfWithoutUserPassword() throws Exception {
        assertThatThrownBy(() -> extractor.extract(file(
                "owner-protected.pdf",
                "application/pdf",
                encryptedPdfBytes("")
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(INVALID_PDF_MESSAGE);
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> extractor.extract(file("empty.txt", "text/plain", new byte[0])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(EMPTY_FILE_MESSAGE);
    }

    @Test
    void rejectsFileLargerThanTenMegabytes() {
        byte[] bytes = new byte[10 * 1024 * 1024 + 1];

        assertThatThrownBy(() -> extractor.extract(file("large.txt", "text/plain", bytes)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(OVERSIZED_FILE_MESSAGE);
    }

    @Test
    void rejectsUnsupportedFilenameExtensionEvenWithPdfMimeType() {
        assertThatThrownBy(() -> extractor.extract(file(
                "notes.json",
                "application/pdf",
                "HashMap".getBytes(StandardCharsets.UTF_8)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(UNSUPPORTED_FILE_MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"csv", "md", "txt"})
    void extractsSupportedPlainTextFormats(String extension) throws Exception {
        MockMultipartFile file = file(
                "hashmap." + extension,
                "application/octet-stream",
                "HashMap 默认负载因子是 0.75。".getBytes(StandardCharsets.UTF_8)
        );

        assertThat(extractor.extract(file)).contains("HashMap");
    }

    private MockMultipartFile file(String filename, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", filename, contentType, bytes);
    }

    private byte[] textPdfBytes(String text) throws Exception {
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

    private byte[] blankPdfBytes() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] encryptedPdfBytes(String userPassword) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(48, 720);
                content.showText("HashMap uses buckets.");
                content.endText();
            }
            AccessPermission permission = new AccessPermission();
            StandardProtectionPolicy policy = new StandardProtectionPolicy(
                    "owner-password",
                    userPassword,
                    permission
            );
            policy.setEncryptionKeyLength(128);
            document.protect(policy);
            document.save(output);
            return output.toByteArray();
        }
    }
}
