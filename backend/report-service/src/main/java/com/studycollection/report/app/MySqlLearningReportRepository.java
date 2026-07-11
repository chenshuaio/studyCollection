package com.studycollection.report.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

@Repository
@Profile("local-mysql")
public class MySqlLearningReportRepository implements LearningReportRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public MySqlLearningReportRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public LearningReportResponse save(Long userId, LearningReportResponse report) {
        String detailsJson = writeDetails(report);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into learning_reports (
                      user_id, weakest_knowledge_point, recommendation, analysis_source,
                      advice_content, details_json, created_at
                    ) values (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setString(2, report.weakestKnowledgePoint());
            statement.setString(3, report.recommendation());
            statement.setString(4, report.adviceSource());
            statement.setString(5, report.adviceContent());
            statement.setString(6, detailsJson);
            statement.setTimestamp(7, Timestamp.from(report.createdAt()));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return report.withIdentity(key == null ? null : key.longValue(), report.createdAt());
    }

    @Override
    public List<LearningReportResponse> findByUserId(Long userId) {
        return jdbcTemplate.query("""
                select id, weakest_knowledge_point, recommendation, analysis_source,
                       advice_content, details_json, created_at
                from learning_reports
                where user_id = ?
                order by created_at desc, id desc
                """, (rs, rowNum) -> {
            ReportDetails details = readDetails(rs.getString("details_json"));
            return new LearningReportResponse(
                    rs.getLong("id"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getString("weakest_knowledge_point"),
                    rs.getString("recommendation"),
                    rs.getString("analysis_source"),
                    rs.getString("advice_content"),
                    details.answeredQuestionCount(),
                    details.gradedQuestionCount(),
                    details.correctQuestionCount(),
                    details.accuracy(),
                    details.knowledgePointPerformance(),
                    details.questionTypePerformance(),
                    details.recentTrend(),
                    details.strengtheningQuestions()
            );
        }, userId);
    }

    private String writeDetails(LearningReportResponse report) {
        try {
            return objectMapper.writeValueAsString(new ReportDetails(
                    report.answeredQuestionCount(),
                    report.gradedQuestionCount(),
                    report.correctQuestionCount(),
                    report.accuracy(),
                    report.knowledgePointPerformance(),
                    report.questionTypePerformance(),
                    report.recentTrend(),
                    report.strengtheningQuestions()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("学习报告序列化失败", exception);
        }
    }

    private ReportDetails readDetails(String json) {
        if (json == null || json.isBlank()) {
            return ReportDetails.empty();
        }
        try {
            return objectMapper.readValue(json, ReportDetails.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("学习报告读取失败", exception);
        }
    }

    public record ReportDetails(
            int answeredQuestionCount,
            int gradedQuestionCount,
            int correctQuestionCount,
            double accuracy,
            List<PerformanceBreakdown> knowledgePointPerformance,
            List<PerformanceBreakdown> questionTypePerformance,
            List<TrendPoint> recentTrend,
            List<StrengtheningQuestion> strengtheningQuestions
    ) {
        public ReportDetails {
            knowledgePointPerformance = safeCopy(knowledgePointPerformance);
            questionTypePerformance = safeCopy(questionTypePerformance);
            recentTrend = safeCopy(recentTrend);
            strengtheningQuestions = safeCopy(strengtheningQuestions);
        }

        static ReportDetails empty() {
            return new ReportDetails(0, 0, 0, 0, List.of(), List.of(), List.of(), List.of());
        }

        private static <T> List<T> safeCopy(List<T> items) {
            return items == null ? List.of() : List.copyOf(items);
        }
    }
}
