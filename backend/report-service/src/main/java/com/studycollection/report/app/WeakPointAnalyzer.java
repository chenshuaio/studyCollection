package com.studycollection.report.app;

import com.studycollection.exam.app.LearningAttempt;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class WeakPointAnalyzer {
    public LearningReport analyze(List<LearningAttempt> attempts) {
        List<LearningAttempt> objectiveAttempts = attempts.stream()
                .filter(LearningAttempt::autoGraded)
                .toList();
        String weakest = objectiveAttempts.isEmpty()
                ? mostFrequentKnowledgePoint(attempts)
                : weakestObjectiveKnowledgePoint(objectiveAttempts);
        double weakestAccuracy = accuracy(objectiveAttempts.stream()
                .filter(attempt -> attempt.knowledgePoint().equals(weakest))
                .toList());
        long percentage = Math.round(weakestAccuracy * 100);

        return new LearningReport(
                weakest,
                "建议优先强化 " + weakest + "（当前客观题正确率 " + percentage + "%），并通过定向练习和错题复盘巩固。"
        );
    }

    private String weakestObjectiveKnowledgePoint(List<LearningAttempt> attempts) {
        return attempts.stream()
                .collect(Collectors.groupingBy(LearningAttempt::knowledgePoint))
                .entrySet()
                .stream()
                .min(Comparator.<Map.Entry<String, List<LearningAttempt>>>comparingDouble(entry -> accuracy(entry.getValue()))
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .orElse("暂无数据");
    }

    private String mostFrequentKnowledgePoint(List<LearningAttempt> attempts) {
        return attempts.stream()
                .map(LearningAttempt::knowledgePoint)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .orElse("暂无数据");
    }

    private double accuracy(List<LearningAttempt> attempts) {
        long correct = attempts.stream().filter(attempt -> Boolean.TRUE.equals(attempt.correct())).count();
        return attempts.isEmpty() ? 0.0 : (double) correct / attempts.size();
    }
}
