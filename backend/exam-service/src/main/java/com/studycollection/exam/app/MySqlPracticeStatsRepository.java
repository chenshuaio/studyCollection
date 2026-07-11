package com.studycollection.exam.app;

import com.studycollection.exam.api.PracticeStats;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local-mysql")
public class MySqlPracticeStatsRepository implements PracticeStatsRepository {
    private final JdbcTemplate jdbcTemplate;

    public MySqlPracticeStatsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PracticeStats add(
            Long userId,
            int answeredQuestionCount,
            int gradedQuestionCount,
            int correctQuestionCount
    ) {
        jdbcTemplate.update("""
                insert into practice_stats (
                  user_id, answered_question_count, graded_question_count, correct_question_count
                ) values (?, ?, ?, ?)
                on duplicate key update
                  answered_question_count = answered_question_count + values(answered_question_count),
                  graded_question_count = graded_question_count + values(graded_question_count),
                  correct_question_count = correct_question_count + values(correct_question_count)
                """, userId, answeredQuestionCount, gradedQuestionCount, correctQuestionCount);
        return findByUserId(userId);
    }

    @Override
    public PracticeStats findByUserId(Long userId) {
        return jdbcTemplate.query(
                        """
                        select user_id, answered_question_count, graded_question_count, correct_question_count
                        from practice_stats
                        where user_id = ?
                        """,
                        (rs, rowNum) -> new PracticeStats(
                                rs.getLong("user_id"),
                                rs.getInt("answered_question_count"),
                                rs.getInt("graded_question_count"),
                                rs.getInt("correct_question_count")
                        ),
                        userId
                ).stream()
                .findFirst()
                .orElse(new PracticeStats(userId, 0, 0, 0));
    }
}
