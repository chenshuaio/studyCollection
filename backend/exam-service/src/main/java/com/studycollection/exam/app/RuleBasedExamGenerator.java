package com.studycollection.exam.app;

import com.studycollection.exam.domain.ExamRule;
import com.studycollection.question.app.QuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class RuleBasedExamGenerator {
    private final QuestionRepository questionRepository;

    public RuleBasedExamGenerator(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public List<Question> generate(ExamRule rule) {
        List<Question> eligible = questionRepository.search(null, null, null, null).stream()
                .filter(question -> rule.knowledgePoints().isEmpty()
                        || rule.knowledgePoints().contains(question.knowledgePoint()))
                .filter(question -> question.answer() != null && !question.answer().isBlank())
                .toList();

        Map<QuestionType, Map<Difficulty, List<Question>>> candidates = groupCandidates(eligible);
        List<QuestionType> types = rule.typeQuotas().entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .toList();
        List<Difficulty> difficulties = rule.difficultyQuotas().entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .toList();
        int[][] allocation = new int[types.size()][difficulties.size()];
        int[] remainingDifficulties = difficulties.stream()
                .mapToInt(rule::difficultyQuota)
                .toArray();

        if (!allocateType(0, types, difficulties, rule, candidates, allocation, remainingDifficulties)) {
            throw new IllegalArgumentException("题库不足，无法同时满足题型与难度配额");
        }

        List<Question> selected = new ArrayList<>(rule.totalQuestions());
        for (int typeIndex = 0; typeIndex < types.size(); typeIndex++) {
            for (int difficultyIndex = 0; difficultyIndex < difficulties.size(); difficultyIndex++) {
                int count = allocation[typeIndex][difficultyIndex];
                if (count == 0) {
                    continue;
                }
                List<Question> cell = new ArrayList<>(candidates
                        .getOrDefault(types.get(typeIndex), Map.of())
                        .getOrDefault(difficulties.get(difficultyIndex), List.of()));
                Collections.shuffle(cell);
                selected.addAll(cell.subList(0, count));
            }
        }
        Collections.shuffle(selected);
        return List.copyOf(selected);
    }

    private boolean allocateType(
            int typeIndex,
            List<QuestionType> types,
            List<Difficulty> difficulties,
            ExamRule rule,
            Map<QuestionType, Map<Difficulty, List<Question>>> candidates,
            int[][] allocation,
            int[] remainingDifficulties
    ) {
        if (typeIndex == types.size()) {
            for (int remaining : remainingDifficulties) {
                if (remaining != 0) {
                    return false;
                }
            }
            return true;
        }
        return allocateDifficulty(
                typeIndex,
                0,
                rule.typeQuota(types.get(typeIndex)),
                types,
                difficulties,
                rule,
                candidates,
                allocation,
                remainingDifficulties
        );
    }

    private boolean allocateDifficulty(
            int typeIndex,
            int difficultyIndex,
            int remainingTypeQuota,
            List<QuestionType> types,
            List<Difficulty> difficulties,
            ExamRule rule,
            Map<QuestionType, Map<Difficulty, List<Question>>> candidates,
            int[][] allocation,
            int[] remainingDifficulties
    ) {
        if (difficultyIndex == difficulties.size()) {
            return remainingTypeQuota == 0 && allocateType(
                    typeIndex + 1,
                    types,
                    difficulties,
                    rule,
                    candidates,
                    allocation,
                    remainingDifficulties
            );
        }

        QuestionType type = types.get(typeIndex);
        Difficulty difficulty = difficulties.get(difficultyIndex);
        int available = candidates
                .getOrDefault(type, Map.of())
                .getOrDefault(difficulty, List.of())
                .size();
        int maximum = Math.min(remainingTypeQuota, Math.min(remainingDifficulties[difficultyIndex], available));
        int minimum = difficultyIndex == difficulties.size() - 1 ? remainingTypeQuota : 0;

        for (int count = maximum; count >= minimum; count--) {
            allocation[typeIndex][difficultyIndex] = count;
            remainingDifficulties[difficultyIndex] -= count;
            if (allocateDifficulty(
                    typeIndex,
                    difficultyIndex + 1,
                    remainingTypeQuota - count,
                    types,
                    difficulties,
                    rule,
                    candidates,
                    allocation,
                    remainingDifficulties
            )) {
                return true;
            }
            remainingDifficulties[difficultyIndex] += count;
            allocation[typeIndex][difficultyIndex] = 0;
        }
        return false;
    }

    private Map<QuestionType, Map<Difficulty, List<Question>>> groupCandidates(List<Question> questions) {
        Map<QuestionType, Map<Difficulty, List<Question>>> grouped = new EnumMap<>(QuestionType.class);
        for (Question question : questions) {
            grouped.computeIfAbsent(question.type(), ignored -> new EnumMap<>(Difficulty.class))
                    .computeIfAbsent(question.difficulty(), ignored -> new ArrayList<>())
                    .add(question);
        }
        return grouped;
    }
}
