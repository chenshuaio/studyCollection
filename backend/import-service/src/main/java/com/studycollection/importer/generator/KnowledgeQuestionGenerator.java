package com.studycollection.importer.generator;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeQuestionGenerator {
    public GeneratedQuestionBank generate(String content) {
        String normalized = content == null ? "" : content;
        List<GeneratedQuestion> questions = new ArrayList<>();

        if (containsAny(normalized, "HashMap", "哈希表")) {
            questions.add(new GeneratedQuestion(
                    "HashMap 默认负载因子是多少？",
                    "SINGLE_CHOICE",
                    "INTERMEDIATE",
                    "集合框架",
                    "A",
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
}
