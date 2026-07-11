package com.studycollection.exam.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
@Profile("local-mysql")
public class MySqlLearningAttemptRepository implements LearningAttemptRepository {
    private final JdbcTemplate jdbcTemplate;

    public MySqlLearningAttemptRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveAll(List<LearningAttempt> attempts) {
        attempts.forEach(attempt -> jdbcTemplate.update("""
                insert into learning_attempts (
                  user_id, activity_type, reference_id, question_id, question_title,
                  question_type, difficulty, knowledge_point, submitted_answer,
                  auto_graded, correct, score, attempted_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on duplicate key update id = id
                """,
                attempt.userId(),
                attempt.activityType().name(),
                attempt.referenceId(),
                attempt.questionId(),
                attempt.questionTitle(),
                attempt.questionType().name(),
                attempt.difficulty().name(),
                attempt.knowledgePoint(),
                attempt.submittedAnswer(),
                attempt.autoGraded(),
                attempt.correct(),
                attempt.score(),
                Timestamp.from(attempt.attemptedAt())
        ));
    }

    @Override
    public List<LearningAttempt> findByUserId(Long userId) {
        return jdbcTemplate.query("""
                select id, user_id, activity_type, reference_id, question_id, question_title,
                       question_type, difficulty, knowledge_point, submitted_answer,
                       auto_graded, correct, score, attempted_at
                from learning_attempts
                where user_id = ?
                order by attempted_at asc, id asc
                """, (rs, rowNum) -> new LearningAttempt(
                rs.getLong("id"),
                rs.getLong("user_id"),
                LearningActivityType.valueOf(rs.getString("activity_type")),
                rs.getString("reference_id"),
                rs.getLong("question_id"),
                rs.getString("question_title"),
                QuestionType.valueOf(rs.getString("question_type")),
                Difficulty.valueOf(rs.getString("difficulty")),
                rs.getString("knowledge_point"),
                rs.getString("submitted_answer"),
                rs.getBoolean("auto_graded"),
                rs.getObject("correct", Boolean.class),
                rs.getInt("score"),
                rs.getTimestamp("attempted_at").toInstant()
        ), userId);
    }
}
