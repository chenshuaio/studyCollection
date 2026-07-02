package com.studycollection.importer.parser;

import java.util.ArrayList;
import java.util.List;

public class MarkdownQuestionParser {
    public ImportPreview parse(String markdown) {
        String[] lines = markdown.split("\\R");
        String title = "";
        String answer = "";
        String knowledgePoint = "";
        String difficulty = "";
        List<String> optionLines = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("题目:")) {
                title = trimmed.substring("题目:".length()).trim();
            } else if (trimmed.startsWith("棰樼洰:")) {
                title = trimmed.substring("棰樼洰:".length()).trim();
            } else if (trimmed.matches("^[A-Da-d][.、．]\\s*.+$")) {
                optionLines.add(normalizeOptionLine(trimmed));
            } else if (trimmed.startsWith("答案:")) {
                answer = trimmed.substring("答案:".length()).trim();
            } else if (trimmed.startsWith("绛旀:")) {
                answer = trimmed.substring("绛旀:".length()).trim();
            } else if (trimmed.startsWith("知识点:")) {
                knowledgePoint = trimmed.substring("知识点:".length()).trim();
            } else if (trimmed.startsWith("鐭ヨ瘑鐐?")) {
                knowledgePoint = trimmed.substring("鐭ヨ瘑鐐?".length()).trim();
            } else if (trimmed.startsWith("难度:")) {
                difficulty = trimmed.substring("难度:".length()).trim();
            } else if (trimmed.startsWith("闅惧害:")) {
                difficulty = trimmed.substring("闅惧害:".length()).trim();
            }
        }

        List<ParsedQuestion> questions = new ArrayList<>();
        if (!title.isBlank()) {
            if (!optionLines.isEmpty()) {
                title = title + System.lineSeparator() + String.join(System.lineSeparator(), optionLines);
            }
            questions.add(new ParsedQuestion(title, answer, knowledgePoint, difficulty));
        }
        return new ImportPreview(questions);
    }

    private String normalizeOptionLine(String line) {
        return line.substring(0, 1).toUpperCase() + ". " + line.substring(2).trim();
    }
}
