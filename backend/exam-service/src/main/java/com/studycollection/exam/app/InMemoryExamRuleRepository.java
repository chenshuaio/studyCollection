package com.studycollection.exam.app;

import com.studycollection.exam.domain.ExamRule;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("!local-mysql")
public class InMemoryExamRuleRepository implements ExamRuleRepository {
    private final AtomicLong ids = new AtomicLong(1);
    private final Map<Long, ExamRule> rules = new LinkedHashMap<>();

    @Override
    public synchronized ExamRule save(ExamRule rule) {
        Long id = rule.id() == null ? ids.getAndIncrement() : rule.id();
        ExamRule saved = rule.withId(id);
        rules.put(id, saved);
        ids.updateAndGet(current -> Math.max(current, id + 1));
        return saved;
    }

    @Override
    public synchronized ExamRule findById(Long id) {
        ExamRule rule = rules.get(id);
        if (rule == null) {
            throw new IllegalArgumentException("考试规则不存在");
        }
        return rule;
    }

    @Override
    public synchronized List<ExamRule> findAll() {
        List<ExamRule> result = new ArrayList<>(rules.values());
        result.sort(Comparator.comparing(ExamRule::updatedAt)
                .reversed()
                .thenComparing(Comparator.comparing(ExamRule::id).reversed()));
        return List.copyOf(result);
    }

    @Override
    public synchronized void deleteById(Long id) {
        if (rules.remove(id) == null) {
            throw new IllegalArgumentException("考试规则不存在");
        }
    }
}
