package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.PendingQuestion;
import com.studycollection.question.domain.PendingQuestionStatus;
import com.studycollection.question.domain.QuestionBankScope;
import com.studycollection.question.domain.QuestionType;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
@Profile("local-mysql")
public class MySqlPendingQuestionRepository implements PendingQuestionRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<PendingQuestion> rowMapper = (rs, rowNum) -> new PendingQuestion(
            rs.getLong("id"),
            rs.getLong("submitter_user_id"),
            rs.getString("title"),
            QuestionType.valueOf(rs.getString("type")),
            Difficulty.valueOf(rs.getString("difficulty")),
            rs.getString("knowledge_point"),
            rs.getString("answer"),
            rs.getString("analysis"),
            QuestionBankScope.valueOf(rs.getString("target_scope")),
            PendingQuestionStatus.valueOf(rs.getString("status"))
    );

    public MySqlPendingQuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PendingQuestion save(PendingQuestion question) {
        if (question.id() != null) {
            int updated = jdbcTemplate.update("""
                    update pending_questions
                    set submitter_user_id = ?, title = ?, type = ?, difficulty = ?, knowledge_point = ?,
                        answer = ?, analysis = ?, target_scope = ?, status = ?
                    where id = ?
                    """,
                    question.submitterUserId(),
                    question.title(),
                    question.type().name(),
                    question.difficulty().name(),
                    question.knowledgePoint(),
                    question.answer(),
                    question.analysis(),
                    question.targetScope().name(),
                    question.status().name(),
                    question.id());
            if (updated == 0) {
                throw new IllegalArgumentException("待审核题目不存在");
            }
            return question;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into pending_questions
                      (submitter_user_id, title, type, difficulty, knowledge_point, answer, analysis, target_scope, status)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, question.submitterUserId());
            statement.setString(2, question.title());
            statement.setString(3, question.type().name());
            statement.setString(4, question.difficulty().name());
            statement.setString(5, question.knowledgePoint());
            statement.setString(6, question.answer());
            statement.setString(7, question.analysis());
            statement.setString(8, question.targetScope().name());
            statement.setString(9, question.status().name());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return new PendingQuestion(
                key == null ? null : key.longValue(),
                question.submitterUserId(),
                question.title(),
                question.type(),
                question.difficulty(),
                question.knowledgePoint(),
                question.answer(),
                question.analysis(),
                question.targetScope(),
                question.status()
        );
    }

    @Override
    public PendingQuestion find(Long id) {
        return jdbcTemplate.query("""
                        select id, submitter_user_id, title, type, difficulty, knowledge_point, answer, analysis,
                               target_scope, status
                        from pending_questions
                        where id = ?
                        """, rowMapper, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("待审核题目不存在"));
    }

    @Override
    public List<PendingQuestion> findByStatus(PendingQuestionStatus status) {
        return jdbcTemplate.query("""
                select id, submitter_user_id, title, type, difficulty, knowledge_point, answer, analysis,
                       target_scope, status
                from pending_questions
                where status = ?
                order by created_at asc, id asc
                """, rowMapper, status.name());
    }
}
