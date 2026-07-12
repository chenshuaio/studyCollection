package com.studycollection.question.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studycollection.question.domain.FeedbackStatus;
import com.studycollection.question.domain.FeedbackType;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;
import com.studycollection.question.domain.QuestionSnapshot;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Repository
@Profile("local-mysql")
public class MySqlQuestionFeedbackRepository implements QuestionFeedbackRepository {
    private static final String FEEDBACK_FIELDS = """
            select id, user_id, question_id, type, content, submitted_answer,
                   source_context, source_reference, status, created_at,
                   reviewed_by, review_note, reviewed_at
            from question_feedback
            """;
    private static final String REVISION_FIELDS = """
            select id, question_id, feedback_id, related_feedback_ids, admin_user_id,
                   change_summary, review_note, before_snapshot, after_snapshot,
                   scoring_affected, created_at
            from question_revisions
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RowMapper<QuestionFeedback> feedbackRowMapper = (rs, rowNum) -> new QuestionFeedback(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getLong("question_id"),
            FeedbackType.valueOf(rs.getString("type")),
            rs.getString("content"),
            rs.getString("submitted_answer"),
            rs.getString("source_context"),
            rs.getString("source_reference"),
            FeedbackStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("created_at").toInstant(),
            nullableLong(rs.getObject("reviewed_by")),
            rs.getString("review_note"),
            nullableInstant(rs.getTimestamp("reviewed_at"))
    );
    private final RowMapper<QuestionRevision> revisionRowMapper = (rs, rowNum) -> new QuestionRevision(
            rs.getLong("id"),
            rs.getLong("question_id"),
            rs.getLong("feedback_id"),
            readFeedbackIds(rs.getString("related_feedback_ids"), rs.getLong("feedback_id")),
            rs.getLong("admin_user_id"),
            rs.getString("change_summary"),
            rs.getString("review_note"),
            readSnapshot(rs.getString("before_snapshot")),
            readSnapshot(rs.getString("after_snapshot")),
            rs.getBoolean("scoring_affected"),
            rs.getTimestamp("created_at").toInstant()
    );

    public MySqlQuestionFeedbackRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public QuestionFeedback saveFeedback(QuestionFeedback feedback) {
        if (feedback.id() != null) {
            int updated = jdbcTemplate.update("""
                    update question_feedback
                    set user_id = ?, question_id = ?, type = ?, content = ?,
                        submitted_answer = ?, source_context = ?, source_reference = ?,
                        status = ?, reviewed_by = ?, review_note = ?, reviewed_at = ?
                    where id = ?
                    """,
                    feedback.userId(),
                    feedback.questionId(),
                    feedback.type().name(),
                    feedback.content(),
                    feedback.submittedAnswer(),
                    feedback.sourceContext(),
                    feedback.sourceReference(),
                    feedback.status().name(),
                    feedback.reviewedBy(),
                    feedback.reviewNote(),
                    timestamp(feedback.reviewedAt()),
                    feedback.id());
            if (updated == 0) {
                throw new IllegalArgumentException("反馈不存在");
            }
            return feedback;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into question_feedback (
                      user_id, question_id, type, content, submitted_answer,
                      source_context, source_reference, status, created_at,
                      reviewed_by, review_note, reviewed_at
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, feedback.userId());
            statement.setLong(2, feedback.questionId());
            statement.setString(3, feedback.type().name());
            statement.setString(4, feedback.content());
            statement.setString(5, feedback.submittedAnswer());
            statement.setString(6, feedback.sourceContext());
            statement.setString(7, feedback.sourceReference());
            statement.setString(8, feedback.status().name());
            statement.setTimestamp(9, Timestamp.from(feedback.createdAt()));
            statement.setObject(10, feedback.reviewedBy());
            statement.setString(11, feedback.reviewNote());
            statement.setTimestamp(12, timestamp(feedback.reviewedAt()));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return feedback.withId(key == null ? null : key.longValue());
    }

    @Override
    public QuestionFeedback findFeedback(Long feedbackId) {
        return jdbcTemplate.query(
                        FEEDBACK_FIELDS + " where id = ?",
                        feedbackRowMapper,
                        feedbackId
                ).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("反馈不存在"));
    }

    @Override
    public List<QuestionFeedback> findByStatus(FeedbackStatus status) {
        return findByStatuses(Set.of(status));
    }

    @Override
    public List<QuestionFeedback> findByStatuses(Set<FeedbackStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", statuses.stream().map(ignored -> "?").toList());
        Object[] args = statuses.stream().map(FeedbackStatus::name).toArray();
        return jdbcTemplate.query(
                FEEDBACK_FIELDS + " where status in (" + placeholders + ") order by created_at desc, id desc",
                feedbackRowMapper,
                args
        );
    }

    @Override
    public List<QuestionFeedback> findByUserId(Long userId) {
        return jdbcTemplate.query(
                FEEDBACK_FIELDS + " where user_id = ? order by created_at desc, id desc",
                feedbackRowMapper,
                userId
        );
    }

    @Override
    public QuestionRevision saveRevision(QuestionRevision revision) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into question_revisions (
                      question_id, feedback_id, related_feedback_ids, admin_user_id,
                      change_summary, review_note, before_snapshot, after_snapshot,
                      scoring_affected, created_at
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, revision.questionId());
            statement.setLong(2, revision.feedbackId());
            statement.setString(3, writeJson(revision.relatedFeedbackIds()));
            statement.setLong(4, revision.adminUserId());
            statement.setString(5, revision.changeSummary());
            statement.setString(6, revision.reviewNote());
            statement.setString(7, writeJson(revision.beforeQuestion()));
            statement.setString(8, writeJson(revision.afterQuestion()));
            statement.setBoolean(9, revision.scoringAffected());
            statement.setTimestamp(10, Timestamp.from(revision.revisedAt()));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return revision.withId(key == null ? null : key.longValue());
    }

    @Override
    public List<QuestionRevision> findRevisionsByQuestionId(Long questionId) {
        return jdbcTemplate.query(
                REVISION_FIELDS + " where question_id = ? order by created_at desc, id desc",
                revisionRowMapper,
                questionId
        );
    }

    @Override
    public Set<Long> findScoringAffectedQuestionIds() {
        return new LinkedHashSet<>(jdbcTemplate.queryForList(
                "select distinct question_id from question_revisions where scoring_affected = true",
                Long.class
        ));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("题目修订记录序列化失败", exception);
        }
    }

    private QuestionSnapshot readSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, QuestionSnapshot.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("题目修订快照读取失败", exception);
        }
    }

    private List<Long> readFeedbackIds(String json, Long fallbackId) {
        if (json == null || json.isBlank()) {
            return List.of(fallbackId);
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("关联反馈记录读取失败", exception);
        }
    }

    private static Timestamp timestamp(java.time.Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static java.time.Instant nullableInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Long nullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}
