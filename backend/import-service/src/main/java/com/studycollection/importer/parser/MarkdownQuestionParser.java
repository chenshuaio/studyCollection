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

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("题目:")) {
                title = trimmed.substring("题目:".length()).trim();
            } else if (trimmed.startsWith("答案:")) {
                answer = trimmed.substring("答案:".length()).trim();
            } else if (trimmed.startsWith("知识点:")) {
                knowledgePoint = trimmed.substring("知识点:".length()).trim();
            } else if (trimmed.startsWith("难度:")) {
                difficulty = trimmed.substring("难度:".length()).trim();
            }
        }

        List<ParsedQuestion> questions = new ArrayList<>();
        if (!title.isBlank()) {
            questions.add(new ParsedQuestion(title, answer, knowledgePoint, difficulty));
        }
        return new ImportPreview(questions);
    }
}
