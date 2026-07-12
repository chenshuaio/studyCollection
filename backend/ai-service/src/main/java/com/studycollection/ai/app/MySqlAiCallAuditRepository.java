package com.studycollection.ai.app;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

@Repository
@Profile("local-mysql")
public class MySqlAiCallAuditRepository implements AiCallAuditRepository {
    private final JdbcTemplate jdbcTemplate;

    public MySqlAiCallAuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AiCallAudit save(AiCallAudit audit) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            insert into ai_call_audits
                                (user_id, purpose, provider, model_name, status, failure_reason, duration_ms, created_at)
                            values (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setObject(1, audit.userId());
            statement.setString(2, audit.purpose());
            statement.setString(3, audit.provider());
            statement.setString(4, audit.modelName());
            statement.setString(5, audit.status());
            statement.setString(6, audit.failureReason());
            statement.setLong(7, audit.durationMs());
            statement.setTimestamp(8, Timestamp.from(audit.createdAt()));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return new AiCallAudit(
                key == null ? null : key.longValue(),
                audit.userId(),
                audit.purpose(),
                audit.provider(),
                audit.modelName(),
                audit.status(),
                audit.failureReason(),
                audit.durationMs(),
                audit.createdAt()
        );
    }

    @Override
    public List<AiCallAudit> findRecent(int limit) {
        return jdbcTemplate.query(
                """
                        select id, user_id, purpose, provider, model_name, status,
                               failure_reason, duration_ms, created_at
                        from ai_call_audits
                        order by created_at desc, id desc
                        limit ?
                        """,
                (resultSet, rowNum) -> new AiCallAudit(
                        resultSet.getLong("id"),
                        resultSet.getObject("user_id", Long.class),
                        resultSet.getString("purpose"),
                        resultSet.getString("provider"),
                        resultSet.getString("model_name"),
                        resultSet.getString("status"),
                        resultSet.getString("failure_reason"),
                        resultSet.getLong("duration_ms"),
                        resultSet.getTimestamp("created_at").toInstant()
                ),
                limit
        );
    }
}
