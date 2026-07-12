package com.studycollection.exam.domain;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record ExamRule(
        Long id,
        String name,
        String description,
        int durationMinutes,
        int totalQuestions,
        List<String> knowledgePoints,
        Map<QuestionType, Integer> typeQuotas,
        Map<Difficulty, Integer> difficultyQuotas,
        ExamRuleStatus status,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public ExamRule {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("考试名称不能为空");
        }
        if (name.trim().length() > 128) {
            throw new IllegalArgumentException("考试名称不能超过 128 个字符");
        }
        if (durationMinutes < 1 || durationMinutes > 480) {
            throw new IllegalArgumentException("考试时长必须在 1 到 480 分钟之间");
        }
        if (totalQuestions < 1 || totalQuestions > 200) {
            throw new IllegalArgumentException("考试题量必须在 1 到 200 题之间");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("创建人不能为空");
        }
        if (status == null || createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("考试规则状态和时间不能为空");
        }

        name = name.trim();
        description = description == null ? "" : description.trim();
        knowledgePoints = normalizeKnowledgePoints(knowledgePoints);
        typeQuotas = normalizeTypeQuotas(typeQuotas);
        difficultyQuotas = normalizeDifficultyQuotas(difficultyQuotas);

        if (sum(typeQuotas) != totalQuestions) {
            throw new IllegalArgumentException("题型配额之和必须等于总题量");
        }
        if (sum(difficultyQuotas) != totalQuestions) {
            throw new IllegalArgumentException("难度配额之和必须等于总题量");
        }
    }

    public ExamRule withId(Long persistedId) {
        return new ExamRule(
                persistedId,
                name,
                description,
                durationMinutes,
                totalQuestions,
                knowledgePoints,
                typeQuotas,
                difficultyQuotas,
                status,
                createdBy,
                createdAt,
                updatedAt
        );
    }

    public int typeQuota(QuestionType type) {
        return typeQuotas.getOrDefault(type, 0);
    }

    public int difficultyQuota(Difficulty difficulty) {
        return difficultyQuotas.getOrDefault(difficulty, 0);
    }

    private static List<String> normalizeKnowledgePoints(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("知识点范围不能包含空值");
        }
        return values.stream().map(String::trim).distinct().toList();
    }

    private static Map<QuestionType, Integer> normalizeTypeQuotas(Map<QuestionType, Integer> values) {
        if (values == null) {
            throw new IllegalArgumentException("题型配额不能为空");
        }
        if (values.keySet().stream().anyMatch(key -> key == null)) {
            throw new IllegalArgumentException("题型配额不能包含空键");
        }
        EnumMap<QuestionType, Integer> normalized = new EnumMap<>(QuestionType.class);
        for (QuestionType type : QuestionType.values()) {
            normalized.put(type, requireNonNegative(values.getOrDefault(type, 0), "题型配额"));
        }
        return Collections.unmodifiableMap(normalized);
    }

    private static Map<Difficulty, Integer> normalizeDifficultyQuotas(Map<Difficulty, Integer> values) {
        if (values == null) {
            throw new IllegalArgumentException("难度配额不能为空");
        }
        if (values.keySet().stream().anyMatch(key -> key == null)) {
            throw new IllegalArgumentException("难度配额不能包含空键");
        }
        EnumMap<Difficulty, Integer> normalized = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            normalized.put(difficulty, requireNonNegative(values.getOrDefault(difficulty, 0), "难度配额"));
        }
        return Collections.unmodifiableMap(normalized);
    }

    private static int requireNonNegative(Integer value, String label) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(label + "不能为负数或空值");
        }
        return value;
    }

    private static long sum(Map<?, Integer> quotas) {
        return quotas.values().stream().mapToLong(Integer::longValue).sum();
    }
}
