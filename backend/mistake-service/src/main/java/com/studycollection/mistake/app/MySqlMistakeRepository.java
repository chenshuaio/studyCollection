package com.studycollection.mistake.app;

import com.studycollection.mistake.domain.MistakeFilter;
import com.studycollection.mistake.domain.MistakeRecord;
import com.studycollection.question.domain.QuestionType;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Repository
@Profile("local-mysql")
public class MySqlMistakeRepository implements MistakeRepository {
    private static final String SELECT_FIELDS = """
            select user_id, question_id, question_title, question_type, knowledge_point,
                   last_submitted_answer, source_context, status, wrong_count,
                   first_wrong_at, last_wrong_at
            from mistake_records
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<MistakeRecord> rowMapper = (rs, rowNum) -> new MistakeRecord(
            rs.getLong("user_id"),
            rs.getLong("question_id"),
            rs.getString("question_title"),
            QuestionType.valueOf(rs.getString("question_type")),
            rs.getString("knowledge_point"),
            rs.getString("last_submitted_answer"),
            rs.getString("source_context"),
            rs.getString("status"),
            rs.getInt("wrong_count"),
            rs.getTimestamp("first_wrong_at").toInstant(),
            rs.getTimestamp("last_wrong_at").toInstant()
    );

    public MySqlMistakeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MistakeRecord recordOccurrence(MistakeRecord record) {
        jdbcTemplate.update("""
                insert into mistake_records (
                  user_id, question_id, question_title, question_type, knowledge_point,
                  last_submitted_answer, source_context, status, wrong_count,
                  first_wrong_at, last_wrong_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on duplicate key update
                  question_title = values(question_title),
                  question_type = values(question_type),
                  knowledge_point = values(knowledge_point),
                  last_submitted_answer = values(last_submitted_answer),
                  source_context = values(source_context),
                  status = 'PENDING',
                  wrong_count = mistake_records.wrong_count + 1,
                  last_wrong_at = values(last_wrong_at)
                """,
                record.userId(),
                record.questionId(),
                record.questionTitle(),
                record.questionType().name(),
                record.knowledgePoint(),
                record.lastSubmittedAnswer(),
                record.sourceContext(),
                record.status(),
                record.wrongCount(),
                Timestamp.from(record.firstWrongAt()),
                Timestamp.from(record.lastWrongAt()));
        return find(record.userId(), record.questionId());
    }

    @Override
    public MistakeRecord updateStatus(Long userId, Long questionId, String status) {
        int updated = jdbcTemplate.update("""
                update mistake_records
                set status = ?
                where user_id = ? and question_id = ?
                """, status, userId, questionId);
        if (updated == 0) {
            throw new IllegalArgumentException("错题记录不存在");
        }
        return find(userId, questionId);
    }

    @Override
    public MistakeRecord find(Long userId, Long questionId) {
        return jdbcTemplate.query(
                        SELECT_FIELDS + " where user_id = ? and question_id = ?",
                        rowMapper,
                        userId,
                        questionId
                ).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("错题记录不存在"));
    }

    @Override
    public List<MistakeRecord> findByUserId(Long userId, MistakeFilter filter) {
        StringBuilder sql = new StringBuilder(SELECT_FIELDS).append(" where user_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (filter.knowledgePoint() != null && !filter.knowledgePoint().isBlank()) {
            sql.append(" and knowledge_point = ?");
            args.add(filter.knowledgePoint());
        }
        if (filter.questionType() != null) {
            sql.append(" and question_type = ?");
            args.add(filter.questionType().name());
        }
        if (filter.status() != null && !filter.status().isBlank()) {
            sql.append(" and status = ?");
            args.add(filter.status());
        }
        if (filter.wrongFrom() != null) {
            sql.append(" and last_wrong_at >= ?");
            args.add(Timestamp.from(filter.wrongFrom()));
        }
        if (filter.wrongToExclusive() != null) {
            sql.append(" and last_wrong_at < ?");
            args.add(Timestamp.from(filter.wrongToExclusive()));
        }
        sql.append(" order by last_wrong_at desc, id desc");
        return jdbcTemplate.query(sql.toString(), rowMapper, args.toArray());
    }
}
