package com.studycollection.exam.app;

import com.studycollection.exam.domain.ExamRule;

import java.util.List;

public interface ExamRuleRepository {
    ExamRule save(ExamRule rule);

    ExamRule findById(Long id);

    List<ExamRule> findAll();

    void deleteById(Long id);
}
