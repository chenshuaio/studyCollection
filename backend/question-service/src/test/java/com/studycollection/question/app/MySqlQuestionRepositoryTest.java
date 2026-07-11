package com.studycollection.question.app;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlQuestionRepositoryTest {
    @Test
    void deleteMarksQuestionAsDeletedSoReferencesCanBePreserved() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        MySqlQuestionRepository repository = new MySqlQuestionRepository(jdbcTemplate);

        repository.deleteById(23L);

        assertThat(jdbcTemplate.sql).contains("update questions set deleted = true");
        assertThat(jdbcTemplate.arguments).containsExactly(23L);
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private String sql;
        private Object[] arguments;

        @Override
        public int update(String sql, Object... args) {
            this.sql = sql;
            this.arguments = args;
            return 1;
        }
    }
}
