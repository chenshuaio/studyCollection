package com.studycollection.importer.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class QuestionDraftFactory {
    ParsedQuestion create(int questionNumber, Map<String, String> source) {
        Map<String, String> fields = normalizeFields(source);
        String title = value(fields, "title", "题目", "题干");
        require(questionNumber, "题目", title);

        List<String> options = options(fields);
        if (!options.isEmpty() && !containsOptions(title)) {
            title = title + System.lineSeparator() + String.join(System.lineSeparator(), options);
        }

        String answer = value(fields, "answer", "答案", "参考答案");
        String knowledgePoint = value(fields, "knowledgepoint", "knowledge", "知识点");
        String difficulty = normalizeDifficulty(value(fields, "difficulty", "难度"), questionNumber);
        String type = normalizeType(value(fields, "type", "题型"), options, questionNumber);
        String analysis = value(fields, "analysis", "explanation", "解析", "答案解析");

        require(questionNumber, "答案", answer);
        require(questionNumber, "知识点", knowledgePoint);
        if (analysis.isBlank()) {
            analysis = "由结构化导入，请管理员审核。";
        }
        return new ParsedQuestion(title, type, difficulty, knowledgePoint, answer, analysis);
    }

    private Map<String, String> normalizeFields(Map<String, String> source) {
        Map<String, String> normalized = new LinkedHashMap<>();
        source.forEach((key, value) -> normalized.put(normalizeKey(key), value == null ? "" : value.trim()));
        return normalized;
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        String normalized = key.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[\\s_\\-]", "");
    }

    private String value(Map<String, String> fields, String... aliases) {
        for (String alias : aliases) {
            String value = fields.get(normalizeKey(alias));
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private List<String> options(Map<String, String> fields) {
        List<String> options = new ArrayList<>();
        for (char label = 'A'; label <= 'D'; label++) {
            String lower = String.valueOf(label).toLowerCase(Locale.ROOT);
            String option = value(fields, "option" + lower, "选项" + lower, lower);
            if (!option.isBlank()) {
                options.add(label + ". " + option);
            }
        }
        return options;
    }

    private boolean containsOptions(String title) {
        return title.matches("(?s).*(?:^|\\R)[A-Da-d][.、．]\\s*.+.*");
    }

    private String normalizeType(String rawType, List<String> options, int questionNumber) {
        if (rawType.isBlank()) {
            return options.size() >= 2 ? "SINGLE_CHOICE" : "FILL_BLANK";
        }
        String normalized = rawType.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "SINGLE_CHOICE", "单选题", "单选" -> "SINGLE_CHOICE";
            case "MULTIPLE_CHOICE", "多选题", "多选" -> "MULTIPLE_CHOICE";
            case "TRUE_FALSE", "判断题", "判断" -> "TRUE_FALSE";
            case "FILL_BLANK", "填空题", "填空" -> "FILL_BLANK";
            case "SHORT_ANSWER", "简答题", "简答" -> "SHORT_ANSWER";
            case "PROGRAMMING", "编程题", "编程" -> "PROGRAMMING";
            default -> throw new IllegalArgumentException("第 " + questionNumber + " 题的题型无效：" + rawType);
        };
    }

    private String normalizeDifficulty(String rawDifficulty, int questionNumber) {
        require(questionNumber, "难度", rawDifficulty);
        String normalized = rawDifficulty.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BEGINNER", "入门", "初级" -> "BEGINNER";
            case "INTERMEDIATE", "进阶", "中级" -> "INTERMEDIATE";
            case "ADVANCED", "精通", "高级" -> "ADVANCED";
            default -> throw new IllegalArgumentException("第 " + questionNumber + " 题的难度无效：" + rawDifficulty);
        };
    }

    private void require(int questionNumber, String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("第 " + questionNumber + " 题缺少" + fieldName);
        }
    }
}
