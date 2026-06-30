package com.studycollection.importer.generator;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeQuestionGenerator {
    private static final List<String> OPTION_LABELS = List.of("A", "B", "C", "D");

    public GeneratedQuestionBank generate(String content) {
        String normalized = content == null ? "" : content;
        List<GeneratedQuestion> questions = new ArrayList<>();

        if (containsAny(normalized, "HashMap", "哈希表")) {
            questions.add(singleChoice(
                    "HashMap 默认负载因子是多少？",
                    "INTERMEDIATE",
                    "集合框架",
                    "0.75",
                    List.of("0.5", "0.75", "1.0", "2.0"),
                    normalized,
                    "HashMap 默认负载因子是 0.75，达到阈值后会触发扩容。"
            ));
        }

        if (containsAny(normalized, "局部变量", "默认值")) {
            questions.add(new GeneratedQuestion(
                    "Java 中局部变量可以不赋值就直接使用，这个说法是否正确？",
                    "TRUE_FALSE",
                    "BEGINNER",
                    "Java 基础",
                    "错误",
                    "Java 局部变量没有默认值，必须先赋值再使用。"
            ));
        }

        if (containsAny(normalized, "封装", "继承", "多态")) {
            questions.add(new GeneratedQuestion(
                    "Java 面向对象三大特性通常指什么？",
                    "SHORT_ANSWER",
                    "BEGINNER",
                    "面向对象",
                    "封装、继承、多态",
                    "Java 面向对象基础中通常把封装、继承、多态作为三大核心特性。"
            ));
        }

        if (questions.isEmpty()) {
            questions.add(new GeneratedQuestion(
                    "请概括这段 Java 学习内容的核心知识点。",
                    "SHORT_ANSWER",
                    "BEGINNER",
                    "Java 综合",
                    "围绕材料中的关键概念作答",
                    "当前内容没有匹配到内置规则，系统先生成概括题，后续可接入在线模型生成更丰富题目。"
            ));
        }

        return new GeneratedQuestionBank(questions);
    }

    private static boolean containsAny(String content, String... keywords) {
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static GeneratedQuestion singleChoice(
            String stem,
            String difficulty,
            String knowledgePoint,
            String correctOption,
            List<String> options,
            String content,
            String analysis
    ) {
        int correctIndex = Math.floorMod((stem + content).hashCode(), OPTION_LABELS.size());
        List<String> arranged = arrangeOptions(correctOption, options, correctIndex);
        String answer = OPTION_LABELS.get(correctIndex);
        return new GeneratedQuestion(
                stem + System.lineSeparator() + formatOptions(arranged),
                "SINGLE_CHOICE",
                difficulty,
                knowledgePoint,
                answer,
                analysis + " 正确选项为 " + answer + "：" + correctOption + "。"
        );
    }

    private static List<String> arrangeOptions(String correctOption, List<String> options, int correctIndex) {
        List<String> distractors = options.stream()
                .filter(option -> !option.equals(correctOption))
                .toList();
        List<String> arranged = new ArrayList<>(List.of("", "", "", ""));
        arranged.set(correctIndex, correctOption);
        int distractorIndex = 0;
        for (int index = 0; index < arranged.size(); index++) {
            if (arranged.get(index).isBlank()) {
                arranged.set(index, distractors.get(distractorIndex++));
            }
        }
        return arranged;
    }

    private static String formatOptions(List<String> options) {
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < options.size(); index++) {
            lines.add(OPTION_LABELS.get(index) + ". " + options.get(index));
        }
        return String.join(System.lineSeparator(), lines);
    }
}
