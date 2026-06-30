package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
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
                    insert into questions (title, type, difficulty, knowledge_point, answer, analysis, source)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, question.title());
            statement.setString(2, question.type().name());
            statement.setString(3, question.difficulty().name());
            statement.setString(4, question.knowledgePoint());
            statement.setString(5, question.answer());
            statement.setString(6, question.analysis());
            statement.setString(7, "LOCAL_UPLOAD");
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return new Question(
                key == null ? question.id() : key.longValue(),
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
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                select id, title, type, difficulty, knowledge_point, answer, analysis
                from questions
                where 1 = 1
                """);
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
                select id, title, type, difficulty, knowledge_point, answer, analysis
                from questions
                where id = ?
                """, rowMapper, id);
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("题目不存在");
        }
        return matches.get(0);
    }

    @Override
    public Question update(Question question) {
        int updated = jdbcTemplate.update("""
                update questions
                set title = ?, type = ?, difficulty = ?, knowledge_point = ?, answer = ?, analysis = ?
                where id = ?
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
        jdbcTemplate.update("delete from questions where id = ?", id);
    }
}
