package com.studycollection.exam.app;

import com.studycollection.exam.domain.ExamAnswer;
import com.studycollection.exam.domain.ExamQuestionSnapshot;
import com.studycollection.exam.domain.ExamSession;
import com.studycollection.exam.domain.ExamStatus;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@Profile("local-mysql")
public class MySqlExamSessionRepository implements ExamSessionRepository {
    private final JdbcTemplate jdbcTemplate;

    public MySqlExamSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public ExamSession create(ExamSession session) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into exam_sessions (
                      user_id, name, duration_minutes, status, started_at, expires_at,
                      submitted_at, score, total_score
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, session.userId());
            statement.setString(2, session.name());
            statement.setInt(3, session.durationMinutes());
            statement.setString(4, session.status().name());
            statement.setTimestamp(5, Timestamp.from(session.startedAt()));
            statement.setTimestamp(6, Timestamp.from(session.expiresAt()));
            statement.setTimestamp(7, timestamp(session.submittedAt()));
            statement.setObject(8, session.score());
            statement.setObject(9, session.totalScore());
            return statement;
        }, keyHolder);
        Number generatedKey = generatedId(keyHolder);
        if (generatedKey == null) {
            throw new IllegalStateException("创建考试记录失败");
        }
        ExamSession persisted = session.withId(generatedKey.longValue());
        insertQuestions(persisted);
        return persisted;
    }

    @Override
    @Transactional
    public ExamSession save(ExamSession session) {
        int updated = jdbcTemplate.update("""
                update exam_sessions
                set status = ?, submitted_at = ?, score = ?, total_score = ?
                where id = ?
                """,
                session.status().name(),
                timestamp(session.submittedAt()),
                session.score(),
                session.totalScore(),
                session.id()
        );
        if (updated == 0) {
            throw new IllegalArgumentException("考试记录不存在");
        }
        jdbcTemplate.update("delete from exam_session_answers where session_id = ?", session.id());
        insertAnswers(session);
        return session;
    }

    @Override
    public ExamSession findById(Long id) {
        SessionRow row = jdbcTemplate.query(
                        """
                        select id, user_id, name, duration_minutes, status, started_at, expires_at,
                               submitted_at, score, total_score
                        from exam_sessions
                        where id = ?
                        """,
                        (rs, rowNum) -> new SessionRow(
                                rs.getLong("id"),
                                rs.getLong("user_id"),
                                rs.getString("name"),
                                rs.getInt("duration_minutes"),
                                ExamStatus.valueOf(rs.getString("status")),
                                rs.getTimestamp("started_at").toInstant(),
                                rs.getTimestamp("expires_at").toInstant(),
                                instant(rs.getTimestamp("submitted_at")),
                                integer(rs.getObject("score")),
                                integer(rs.getObject("total_score"))
                        ),
                        id
                ).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("考试记录不存在"));

        List<ExamQuestionSnapshot> questions = jdbcTemplate.query(
                """
                select question_id, question_title, question_type, difficulty, knowledge_point,
                       correct_answer, analysis, sort_order
                from exam_session_questions
                where session_id = ?
                order by sort_order asc
                """,
                (rs, rowNum) -> new ExamQuestionSnapshot(
                        rs.getLong("question_id"),
                        rs.getString("question_title"),
                        QuestionType.valueOf(rs.getString("question_type")),
                        Difficulty.valueOf(rs.getString("difficulty")),
                        rs.getString("knowledge_point"),
                        rs.getString("correct_answer"),
                        rs.getString("analysis"),
                        rs.getInt("sort_order")
                ),
                id
        );

        Map<Long, ExamAnswer> answers = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                select question_id, submitted_answer, auto_graded, correct, score
                from exam_session_answers
                where session_id = ?
                """,
                rs -> {
                    Object correct = rs.getObject("correct");
                    ExamAnswer answer = new ExamAnswer(
                            rs.getLong("question_id"),
                            rs.getString("submitted_answer"),
                            rs.getBoolean("auto_graded"),
                            correct == null ? null : rs.getBoolean("correct"),
                            rs.getInt("score")
                    );
                    answers.put(answer.questionId(), answer);
                },
                id
        );

        return new ExamSession(
                row.id(),
                row.userId(),
                row.name(),
                row.durationMinutes(),
                row.status(),
                row.startedAt(),
                row.expiresAt(),
                row.submittedAt(),
                row.score(),
                row.totalScore(),
                questions,
                answers
        );
    }

    @Override
    public List<ExamSession> findByUserId(Long userId) {
        return jdbcTemplate.queryForList(
                        """
                        select id
                        from exam_sessions
                        where user_id = ?
                        order by started_at desc, id desc
                        """,
                        Long.class,
                        userId
                ).stream()
                .map(this::findById)
                .toList();
    }

    private void insertQuestions(ExamSession session) {
        jdbcTemplate.batchUpdate("""
                insert into exam_session_questions (
                  session_id, question_id, question_title, question_type, difficulty,
                  knowledge_point, correct_answer, analysis, sort_order
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                ExamQuestionSnapshot question = session.questions().get(index);
                statement.setLong(1, session.id());
                statement.setLong(2, question.questionId());
                statement.setString(3, question.title());
                statement.setString(4, question.type().name());
                statement.setString(5, question.difficulty().name());
                statement.setString(6, question.knowledgePoint());
                statement.setString(7, question.correctAnswer());
                statement.setString(8, question.analysis());
                statement.setInt(9, question.sortOrder());
            }

            @Override
            public int getBatchSize() {
                return session.questions().size();
            }
        });
    }

    private void insertAnswers(ExamSession session) {
        List<ExamAnswer> answers = List.copyOf(session.answers().values());
        if (answers.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                insert into exam_session_answers (
                  session_id, question_id, submitted_answer, auto_graded, correct, score
                ) values (?, ?, ?, ?, ?, ?)
                """, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                ExamAnswer answer = answers.get(index);
                statement.setLong(1, session.id());
                statement.setLong(2, answer.questionId());
                statement.setString(3, answer.submittedAnswer());
                statement.setBoolean(4, answer.autoGraded());
                statement.setObject(5, answer.correct());
                statement.setInt(6, answer.score());
            }

            @Override
            public int getBatchSize() {
                return answers.size();
            }
        });
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Integer integer(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static Number generatedId(KeyHolder keyHolder) {
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys == null) {
            return null;
        }
        Object namedId = keys.entrySet().stream()
                .filter(entry -> "id".equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (namedId instanceof Number number) {
            return number;
        }
        return keys.values().stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .findFirst()
                .orElse(null);
    }

    private record SessionRow(
            Long id,
            Long userId,
            String name,
            int durationMinutes,
            ExamStatus status,
            Instant startedAt,
            Instant expiresAt,
            Instant submittedAt,
            Integer score,
            Integer totalScore
    ) {
    }
}
