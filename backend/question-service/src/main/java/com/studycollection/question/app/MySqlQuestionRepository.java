package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionBankScope;
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
import java.util.ArrayList;
import java.util.List;

@Primary
@Repository
@Profile("local-mysql")
public class MySqlQuestionRepository implements QuestionRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Question> rowMapper = (rs, rowNum) -> new Question(
            rs.getLong("id"),
            nullableLong(rs, "owner_user_id"),
            rs.getString("title"),
            QuestionType.valueOf(rs.getString("type")),
            Difficulty.valueOf(rs.getString("difficulty")),
            rs.getString("knowledge_point"),
            rs.getString("answer"),
            rs.getString("analysis")
    );

    public MySqlQuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Question save(Question question) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into questions (owner_user_id, title, type, difficulty, knowledge_point, answer, analysis, source)
                    values (?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            if (question.ownerUserId() == null) {
                statement.setNull(1, java.sql.Types.BIGINT);
            } else {
                statement.setLong(1, question.ownerUserId());
            }
            statement.setString(2, question.title());
            statement.setString(3, question.type().name());
            statement.setString(4, question.difficulty().name());
            statement.setString(5, question.knowledgePoint());
            statement.setString(6, question.answer());
            statement.setString(7, question.analysis());
            statement.setString(8, "LOCAL_UPLOAD");
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return new Question(
                key == null ? question.id() : key.longValue(),
                question.ownerUserId(),
                question.title(),
                question.type(),
                question.difficulty(),
                question.knowledgePoint(),
                question.answer(),
                question.analysis()
        );
    }

    @Override
    public List<Question> search(String keyword, String knowledgePoint, Difficulty difficulty, QuestionType type) {
        return searchWithVisibility(null, null, keyword, knowledgePoint, difficulty, type);
    }

    @Override
    public List<Question> searchAccessible(
            Long userId,
            QuestionBankScope scope,
            String keyword,
            String knowledgePoint,
            Difficulty difficulty,
            QuestionType type
    ) {
        return searchWithVisibility(userId, scope == null ? QuestionBankScope.ALL : scope,
                keyword, knowledgePoint, difficulty, type);
    }

    private List<Question> searchWithVisibility(
            Long userId,
            QuestionBankScope scope,
            String keyword,
            String knowledgePoint,
            Difficulty difficulty,
            QuestionType type
    ) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                select id, owner_user_id, title, type, difficulty, knowledge_point, answer, analysis
                from questions
                where deleted = false
                """);
        if (scope != null) {
            if (scope == QuestionBankScope.PUBLIC) {
                sql.append(" and owner_user_id is null");
            } else if (scope == QuestionBankScope.PERSONAL) {
                sql.append(" and owner_user_id = ?");
                args.add(userId);
            } else {
                sql.append(" and (owner_user_id is null or owner_user_id = ?)");
                args.add(userId);
            }
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" and lower(title) like ?");
            args.add("%" + keyword.toLowerCase() + "%");
        }
        if (knowledgePoint != null && !knowledgePoint.isBlank()) {
            sql.append(" and knowledge_point = ?");
            args.add(knowledgePoint);
        }
        if (difficulty != null) {
            sql.append(" and difficulty = ?");
            args.add(difficulty.name());
        }
        if (type != null) {
            sql.append(" and type = ?");
            args.add(type.name());
        }
        sql.append(" order by id desc");
        return jdbcTemplate.query(sql.toString(), rowMapper, args.toArray());
    }

    @Override
    public Question findById(Long id) {
        List<Question> matches = jdbcTemplate.query("""
                select id, owner_user_id, title, type, difficulty, knowledge_point, answer, analysis
                from questions
                where id = ? and deleted = false
                """, rowMapper, id);
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("题目不存在");
        }
        return matches.get(0);
    }

    @Override
    public Question findAccessibleById(Long id, Long userId) {
        return jdbcTemplate.query("""
                        select id, owner_user_id, title, type, difficulty, knowledge_point, answer, analysis
                        from questions
                        where id = ? and deleted = false
                          and (owner_user_id is null or owner_user_id = ?)
                        """, rowMapper, id, userId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("题目不存在或无权访问"));
    }

    @Override
    public String findSourceById(Long id) {
        return jdbcTemplate.query(
                        "select source from questions where id = ? and deleted = false",
                        (rs, rowNum) -> rs.getString("source"),
                        id
                ).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("题目不存在"));
    }

    @Override
    public Question update(Question question) {
        int updated = jdbcTemplate.update("""
                update questions
                set title = ?, type = ?, difficulty = ?, knowledge_point = ?, answer = ?, analysis = ?
                where id = ? and deleted = false
                """,
                question.title(),
                question.type().name(),
                question.difficulty().name(),
                question.knowledgePoint(),
                question.answer(),
                question.analysis(),
                question.id());
        if (updated == 0) {
            throw new IllegalArgumentException("题目不存在");
        }
        return question;
    }

    @Override
    public void deleteById(Long id) {
        int updated = jdbcTemplate.update(
                "update questions set deleted = true where id = ? and deleted = false",
                id
        );
        if (updated == 0) {
            throw new IllegalArgumentException("题目不存在");
        }
    }

    @Override
    public void deleteOwnedById(Long id, Long userId) {
        int updated = jdbcTemplate.update(
                "update questions set deleted = true where id = ? and owner_user_id = ? and deleted = false",
                id,
                userId
        );
        if (updated == 0) {
            throw new IllegalArgumentException("题目不存在或无权访问");
        }
    }

    private static Long nullableLong(java.sql.ResultSet resultSet, String column) throws java.sql.SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
