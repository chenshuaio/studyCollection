package com.studycollection.ai.app;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlAiModelSettingsRepositoryTest {
    @Test
    void savesOnlyNonSensitiveSettings() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        MySqlAiModelSettingsRepository repository = new MySqlAiModelSettingsRepository(jdbcTemplate);

        repository.save(new AiModelSettings(
                "OPENAI_COMPATIBLE",
                "https://example.com/v1/chat/completions",
                "qwen-plus",
                7L,
                Instant.parse("2026-07-12T08:00:00Z")
        ));

        assertThat(jdbcTemplate.sql).contains("ai_model_settings");
        assertThat(jdbcTemplate.sql).doesNotContainIgnoringCase("api_key");
        assertThat(jdbcTemplate.arguments)
                .contains("OPENAI_COMPATIBLE", "https://example.com/v1/chat/completions", "qwen-plus", 7L);
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
