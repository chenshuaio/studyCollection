package com.studycollection.question.app;

import com.studycollection.question.domain.FeedbackStatus;
import com.studycollection.question.domain.FeedbackType;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;
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
public class MySqlQuestionFeedbackRepository implements QuestionFeedbackRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<QuestionFeedback> feedbackRowMapper = (rs, rowNum) -> new QuestionFeedback(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getLong("question_id"),
            FeedbackType.valueOf(rs.getString("type")),
            rs.getString("content"),
            FeedbackStatus.valueOf(rs.getString("status"))
    );

    public MySqlQuestionFeedbackRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public QuestionFeedback saveFeedback(QuestionFeedback feedback) {
        if (feedback.id() != null) {
            int updated = jdbcTemplate.update("""
                    update question_feedback
                    set user_id = ?, question_id = ?, type = ?, content = ?, status = ?
                    where id = ?
                    """,
                    feedback.userId(),
                    feedback.questionId(),
                    feedback.type().name(),
                    feedback.content(),
                    feedback.status().name(),
                    feedback.id());
            if (updated == 0) {
                throw new IllegalArgumentException("反馈不存在");
            }
            return feedback;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into question_feedback (user_id, question_id, type, content, status)
                    values (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, feedback.userId());
            statement.setLong(2, feedback.questionId());
            statement.setString(3, feedback.type().name());
            statement.setString(4, feedback.content());
            statement.setString(5, feedback.status().name());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return new QuestionFeedback(
                key == null ? null : key.longValue(),
                feedback.userId(),
                feedback.questionId(),
                feedback.type(),
                feedback.content(),
                feedback.status()
        );
    }

    @Override
    public QuestionFeedback findFeedback(Long feedbackId) {
        return jdbcTemplate.query("""
                        select id, user_id, question_id, type, content, status
                        from question_feedback
                        where id = ?
                        """, feedbackRowMapper, feedbackId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("反馈不存在"));
    }

    @Override
    public List<QuestionFeedback> findByStatus(FeedbackStatus status) {
        return jdbcTemplate.query("""
                select id, user_id, question_id, type, content, status
                from question_feedback
                where status = ?
                order by created_at asc, id asc
                """, feedbackRowMapper, status.name());
    }

    @Override
    public List<QuestionFeedback> findByUserId(Long userId) {
        return jdbcTemplate.query("""
                select id, user_id, question_id, type, content, status
                from question_feedback
                where user_id = ?
                order by created_at desc, id desc
                """, feedbackRowMapper, userId);
    }

    @Override
    public QuestionRevision saveRevision(QuestionRevision revision) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into question_revisions
                      (question_id, feedback_id, admin_user_id, change_summary, review_note)
                    values (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, revision.questionId());
            statement.setLong(2, revision.feedbackId());
            statement.setLong(3, revision.adminUserId());
            statement.setString(4, revision.changeSummary());
            statement.setString(5, revision.reviewNote());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return new QuestionRevision(
                key == null ? null : key.longValue(),
                revision.questionId(),
                revision.feedbackId(),
                revision.adminUserId(),
                revision.changeSummary(),
                revision.reviewNote()
        );
    }
}
