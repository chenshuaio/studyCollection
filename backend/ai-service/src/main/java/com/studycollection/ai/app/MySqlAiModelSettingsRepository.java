package com.studycollection.ai.app;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("local-mysql")
public class MySqlAiModelSettingsRepository implements AiModelSettingsRepository {
    private final JdbcTemplate jdbcTemplate;

    public MySqlAiModelSettingsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AiModelSettings> find() {
        List<AiModelSettings> results = jdbcTemplate.query(
                """
                        select provider, endpoint, model_name, updated_by, updated_at
                        from ai_model_settings
                        where id = 1
                        """,
                (resultSet, rowNum) -> new AiModelSettings(
                        resultSet.getString("provider"),
                        resultSet.getString("endpoint"),
                        resultSet.getString("model_name"),
                        resultSet.getLong("updated_by"),
                        resultSet.getTimestamp("updated_at").toInstant()
                )
        );
        return results.stream().findFirst();
    }

    @Override
    public AiModelSettings save(AiModelSettings settings) {
        jdbcTemplate.update(
                """
                        insert into ai_model_settings
                            (id, provider, endpoint, model_name, updated_by, updated_at)
                        values (1, ?, ?, ?, ?, ?)
                        on duplicate key update
                            provider = values(provider),
                            endpoint = values(endpoint),
                            model_name = values(model_name),
                            updated_by = values(updated_by),
                            updated_at = values(updated_at)
                        """,
                settings.provider(),
                settings.endpoint(),
                settings.modelName(),
                settings.updatedBy(),
                Timestamp.from(settings.updatedAt())
        );
        return settings;
    }
}
