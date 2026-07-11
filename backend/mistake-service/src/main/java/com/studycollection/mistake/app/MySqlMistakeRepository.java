package com.studycollection.mistake.app;

import com.studycollection.mistake.domain.MistakeRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("local-mysql")
public class MySqlMistakeRepository implements MistakeRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<MistakeRecord> rowMapper = (rs, rowNum) -> new MistakeRecord(
            rs.getLong("user_id"),
            rs.getLong("question_id"),
            rs.getString("question_title"),
            rs.getString("knowledge_point"),
            rs.getString("status")
    );

    public MySqlMistakeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MistakeRecord save(MistakeRecord record) {
        jdbcTemplate.update("""
                insert into mistake_records (user_id, question_id, question_title, knowledge_point, status)
                values (?, ?, ?, ?, ?)
                on duplicate key update
                  question_title = values(question_title),
                  knowledge_point = values(knowledge_point),
                  status = values(status)
                """,
                record.userId(),
                record.questionId(),
                record.questionTitle(),
                record.knowledgePoint(),
                record.status());
        return record;
    }

    @Override
    public MistakeRecord find(Long userId, Long questionId) {
        return jdbcTemplate.query("""
                        select user_id, question_id, question_title, knowledge_point, status
                        from mistake_records
                        where user_id = ? and question_id = ?
                        """, rowMapper, userId, questionId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("错题记录不存在"));
    }

    @Override
    public List<MistakeRecord> findByUserId(Long userId) {
        return jdbcTemplate.query("""
                select user_id, question_id, question_title, knowledge_point, status
                from mistake_records
                where user_id = ?
                order by updated_at desc, id desc
                """, rowMapper, userId);
    }
}
