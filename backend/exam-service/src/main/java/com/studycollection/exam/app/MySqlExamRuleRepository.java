package com.studycollection.exam.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studycollection.exam.domain.ExamRule;
import com.studycollection.exam.domain.ExamRuleStatus;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Primary
@Repository
@Profile("local-mysql")
public class MySqlExamRuleRepository implements ExamRuleRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RowMapper<ExamRule> rowMapper = (rs, rowNum) -> new ExamRule(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getInt("duration_minutes"),
            rs.getInt("total_questions"),
            readKnowledgePoints(rs.getString("knowledge_points")),
            readTypeQuotas(rs.getString("type_quotas")),
            readDifficultyQuotas(rs.getString("difficulty_quotas")),
            ExamRuleStatus.valueOf(rs.getString("status")),
            rs.getLong("created_by"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
    );

    public MySqlExamRuleRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ExamRule save(ExamRule rule) {
        if (rule.id() == null) {
            return insert(rule);
        }
        int updated = jdbcTemplate.update("""
                update exam_rules
                set name = ?, description = ?, duration_minutes = ?, total_questions = ?,
                    knowledge_points = ?, type_quotas = ?, difficulty_quotas = ?, status = ?,
                    updated_at = ?
                where id = ?
                """,
                rule.name(),
                rule.description(),
                rule.durationMinutes(),
                rule.totalQuestions(),
                writeJson(rule.knowledgePoints()),
                writeJson(rule.typeQuotas()),
                writeJson(rule.difficultyQuotas()),
                rule.status().name(),
                Timestamp.from(rule.updatedAt()),
                rule.id());
        if (updated == 0) {
            throw new IllegalArgumentException("考试规则不存在");
        }
        return rule;
    }

    @Override
    public ExamRule findById(Long id) {
        return jdbcTemplate.query("""
                        select id, name, description, duration_minutes, total_questions,
                               knowledge_points, type_quotas, difficulty_quotas, status,
                               created_by, created_at, updated_at
                        from exam_rules
                        where id = ?
                        """, rowMapper, id).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("考试规则不存在"));
    }

    @Override
    public List<ExamRule> findAll() {
        return jdbcTemplate.query("""
                select id, name, description, duration_minutes, total_questions,
                       knowledge_points, type_quotas, difficulty_quotas, status,
                       created_by, created_at, updated_at
                from exam_rules
                order by updated_at desc, id desc
                """, rowMapper);
    }

    @Override
    public void deleteById(Long id) {
        if (jdbcTemplate.update("delete from exam_rules where id = ?", id) == 0) {
            throw new IllegalArgumentException("考试规则不存在");
        }
    }

    private ExamRule insert(ExamRule rule) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into exam_rules (
                      name, description, duration_minutes, total_questions,
                      knowledge_points, type_quotas, difficulty_quotas, status,
                      created_by, created_at, updated_at
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, rule.name());
            statement.setString(2, rule.description());
            statement.setInt(3, rule.durationMinutes());
            statement.setInt(4, rule.totalQuestions());
            statement.setString(5, writeJson(rule.knowledgePoints()));
            statement.setString(6, writeJson(rule.typeQuotas()));
            statement.setString(7, writeJson(rule.difficultyQuotas()));
            statement.setString(8, rule.status().name());
            statement.setLong(9, rule.createdBy());
            statement.setTimestamp(10, Timestamp.from(rule.createdAt()));
            statement.setTimestamp(11, Timestamp.from(rule.updatedAt()));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("考试规则保存失败");
        }
        return rule.withId(key.longValue());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("考试规则配置序列化失败", exception);
        }
    }

    private List<String> readKnowledgePoints(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("考试规则知识点数据损坏", exception);
        }
    }

    private Map<QuestionType, Integer> readTypeQuotas(String json) {
        Map<String, Integer> values = readIntegerMap(json);
        EnumMap<QuestionType, Integer> result = new EnumMap<>(QuestionType.class);
        values.forEach((key, value) -> result.put(QuestionType.valueOf(key), value));
        return result;
    }

    private Map<Difficulty, Integer> readDifficultyQuotas(String json) {
        Map<String, Integer> values = readIntegerMap(json);
        EnumMap<Difficulty, Integer> result = new EnumMap<>(Difficulty.class);
        values.forEach((key, value) -> result.put(Difficulty.valueOf(key), value));
        return result;
    }

    private Map<String, Integer> readIntegerMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("考试规则配额数据损坏", exception);
        }
    }
}
