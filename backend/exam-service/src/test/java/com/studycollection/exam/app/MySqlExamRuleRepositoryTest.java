package com.studycollection.exam.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studycollection.exam.domain.ExamRule;
import com.studycollection.exam.domain.ExamRuleStatus;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlExamRuleRepositoryTest {
    private MySqlExamRuleRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:exam_rules;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "sa",
                ""
        );
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("drop table if exists exam_rules");
        jdbcTemplate.execute("""
                create table exam_rules (
                  id bigint primary key auto_increment,
                  name varchar(128) not null,
                  description text not null,
                  duration_minutes int not null,
                  total_questions int not null,
                  knowledge_points text not null,
                  type_quotas text not null,
                  difficulty_quotas text not null,
                  status varchar(32) not null,
                  created_by bigint not null,
                  created_at timestamp not null,
                  updated_at timestamp not null
                )
                """);
        repository = new MySqlExamRuleRepository(jdbcTemplate, new ObjectMapper());
    }

    @Test
    void roundTripsJsonQuotasUpdatesListsAndDeletes() {
        Instant createdAt = Instant.parse("2026-07-12T03:00:00Z");
        ExamRule created = repository.save(new ExamRule(
                null,
                "Java 综合模拟考试",
                "覆盖基础与集合",
                60,
                4,
                List.of("Java 基础", "集合框架"),
                Map.of(QuestionType.SINGLE_CHOICE, 2, QuestionType.MULTIPLE_CHOICE, 2),
                Map.of(Difficulty.BEGINNER, 2, Difficulty.INTERMEDIATE, 2),
                ExamRuleStatus.DRAFT,
                1L,
                createdAt,
                createdAt
        ));

        ExamRule published = repository.save(new ExamRule(
                created.id(),
                created.name(),
                created.description(),
                created.durationMinutes(),
                created.totalQuestions(),
                created.knowledgePoints(),
                created.typeQuotas(),
                created.difficultyQuotas(),
                ExamRuleStatus.PUBLISHED,
                created.createdBy(),
                created.createdAt(),
                createdAt.plusSeconds(60)
        ));

        ExamRule restored = repository.findById(created.id());
        assertThat(restored).isEqualTo(published);
        assertThat(restored.typeQuota(QuestionType.SINGLE_CHOICE)).isEqualTo(2);
        assertThat(restored.difficultyQuota(Difficulty.INTERMEDIATE)).isEqualTo(2);
        assertThat(repository.findAll()).containsExactly(published);

        repository.deleteById(created.id());
        assertThat(repository.findAll()).isEmpty();
    }
}
