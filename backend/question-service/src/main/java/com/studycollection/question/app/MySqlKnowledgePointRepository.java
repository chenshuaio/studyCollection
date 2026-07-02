package com.studycollection.question.app;

import com.studycollection.question.domain.KnowledgePoint;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Primary
@Repository
@Profile("local-mysql")
public class MySqlKnowledgePointRepository implements KnowledgePointRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<KnowledgePoint> rowMapper = (rs, rowNum) -> new KnowledgePoint(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getBoolean("enabled")
    );

    public MySqlKnowledgePointRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public KnowledgePoint save(KnowledgePoint knowledgePoint) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into knowledge_points (name, description, enabled)
                    values (?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, knowledgePoint.name());
            statement.setString(2, knowledgePoint.description());
            statement.setBoolean(3, knowledgePoint.enabled());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return new KnowledgePoint(
                key == null ? knowledgePoint.id() : key.longValue(),
                knowledgePoint.name(),
                knowledgePoint.description(),
                knowledgePoint.enabled()
        );
    }

    @Override
    public List<KnowledgePoint> findAll() {
        return jdbcTemplate.query("""
                select id, name, description, enabled
                from knowledge_points
                order by id
                """, rowMapper);
    }

    @Override
    public KnowledgePoint disable(Long id) {
        int updated = jdbcTemplate.update("update knowledge_points set enabled = false where id = ?", id);
        if (updated == 0) {
            throw new IllegalArgumentException("知识点不存在");
        }
        return jdbcTemplate.queryForObject("""
                select id, name, description, enabled
                from knowledge_points
                where id = ?
                """, rowMapper, id);
    }
}
