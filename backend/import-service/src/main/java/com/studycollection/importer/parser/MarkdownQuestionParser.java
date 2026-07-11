package com.studycollection.importer.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MarkdownQuestionParser {
    private final QuestionDraftFactory draftFactory = new QuestionDraftFactory();

    public ImportPreview parse(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            throw new IllegalArgumentException("题目内容不能为空");
        }

        List<ParsedQuestion> questions = new ArrayList<>();
        Map<String, String> current = new LinkedHashMap<>();
        String headingType = "";
        for (String line : markdown.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("##")) {
                addCurrent(questions, current);
                current = new LinkedHashMap<>();
                headingType = trimmed.replaceFirst("^#+", "").trim();
                continue;
            }
            if (hasLabel(trimmed, "题目")) {
                addCurrent(questions, current);
                current = new LinkedHashMap<>();
                if (!headingType.isBlank()) {
                    current.put("题型", headingType);
                }
                current.put("题目", labelValue(trimmed, "题目"));
            } else if (trimmed.matches("^[A-Da-d][.、．]\\s*.+$")) {
                current.put("选项" + trimmed.substring(0, 1).toUpperCase(), trimmed.substring(2).trim());
            } else {
                copyLabel(trimmed, current, "题型");
                copyLabel(trimmed, current, "答案");
                copyLabel(trimmed, current, "解析");
                copyLabel(trimmed, current, "知识点");
                copyLabel(trimmed, current, "难度");
            }
        }
        addCurrent(questions, current);
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("未解析到任何题目，请检查是否包含“题目:”字段");
        }
        return new ImportPreview(List.copyOf(questions));
    }

    private void addCurrent(List<ParsedQuestion> questions, Map<String, String> current) {
        if (current.containsKey("题目")) {
            questions.add(draftFactory.create(questions.size() + 1, current));
        }
    }

    private void copyLabel(String line, Map<String, String> fields, String label) {
        if (hasLabel(line, label)) {
            fields.put(label, labelValue(line, label));
        }
    }

    private boolean hasLabel(String line, String label) {
        return line.startsWith(label + ":") || line.startsWith(label + "：");
    }

    private String labelValue(String line, String label) {
        return line.substring(label.length() + 1).trim();
    }
}
