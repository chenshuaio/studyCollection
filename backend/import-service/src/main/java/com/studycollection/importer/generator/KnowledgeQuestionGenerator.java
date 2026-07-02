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

        if (containsAny(normalized, "JVM", "栈", "堆")) {
            questions.add(new GeneratedQuestion(
                    "JVM 栈和堆通常分别保存什么内容？",
                    "SHORT_ANSWER",
                    "INTERMEDIATE",
                    "JVM",
                    "栈保存方法调用栈帧和局部变量等，堆保存对象实例等运行时数据",
                    "JVM 栈更关注线程私有的方法调用过程，堆更关注对象实例和共享运行时数据。"
            ));
        }

        if (containsAny(normalized, "接口", "抽象类")) {
            questions.add(new GeneratedQuestion(
                    "Java 接口和抽象类的核心区别是什么？",
                    "SHORT_ANSWER",
                    "INTERMEDIATE",
                    "面向对象",
                    "接口偏向能力契约，抽象类可以保留共同状态和部分实现",
                    "接口常用于定义能力边界，抽象类常用于抽取共同代码和状态。"
            ));
        }

        if (containsAny(normalized, "异常", "try", "catch", "finally")) {
            questions.add(singleChoice(
                    "Java 中 finally 代码块通常会在什么情况下执行？",
                    "BEGINNER",
                    "异常处理",
                    "try/catch 执行结束后通常会执行",
                    List.of("只在没有异常时执行", "只在 catch 中执行", "try/catch 执行结束后通常会执行", "编译阶段执行"),
                    normalized,
                    "finally 常用于释放资源，通常会在 try/catch 流程结束后执行。"
            ));
        }

        if (containsAny(normalized, "ArrayList", "LinkedList")) {
            questions.add(new GeneratedQuestion(
                    "ArrayList 和 LinkedList 在使用场景上有什么常见差异？",
                    "SHORT_ANSWER",
                    "INTERMEDIATE",
                    "集合框架",
                    "ArrayList 随机访问更快，LinkedList 在链表节点插入删除场景更灵活",
                    "ArrayList 基于动态数组，LinkedList 基于链表结构，选择时应结合访问和增删场景。"
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
