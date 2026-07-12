package com.studycollection.exam.app;

import com.studycollection.exam.domain.ExamRule;
import com.studycollection.exam.domain.ExamRuleStatus;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryExamRuleRepositoryTest {
    @Test
    void listsMostRecentlyUpdatedRuleFirst() {
        InMemoryExamRuleRepository repository = new InMemoryExamRuleRepository();
        ExamRule older = repository.save(rule("较早规则", Instant.parse("2026-07-12T03:00:00Z")));
        ExamRule newer = repository.save(rule("最新规则", Instant.parse("2026-07-12T03:01:00Z")));

        assertThat(repository.findAll()).extracting(ExamRule::id).containsExactly(newer.id(), older.id());
    }

    private ExamRule rule(String name, Instant updatedAt) {
        return new ExamRule(
                null,
                name,
                "",
                30,
                1,
                List.of(),
                Map.of(QuestionType.SINGLE_CHOICE, 1),
                Map.of(Difficulty.BEGINNER, 1),
                ExamRuleStatus.DRAFT,
                1L,
                Instant.parse("2026-07-12T03:00:00Z"),
                updatedAt
        );
    }
}
