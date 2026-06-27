package com.studycollection.report.app;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WeakPointAnalyzer {
    public record Result(String knowledgePoint, boolean correct) {
    }

    public LearningReport analyze(List<Result> results) {
        String weakest = results.stream()
                .collect(Collectors.groupingBy(Result::knowledgePoint))
                .entrySet()
                .stream()
                .min(Comparator.comparingDouble(entry -> accuracy(entry.getValue())))
                .map(Map.Entry::getKey)
                .orElse("暂无数据");

        return new LearningReport(weakest, "建议优先强化 " + weakest + "，并通过错题重练巩固。");
    }

    private double accuracy(List<Result> results) {
        long correct = results.stream().filter(Result::correct).count();
        return results.isEmpty() ? 1.0 : (double) correct / results.size();
    }
}
