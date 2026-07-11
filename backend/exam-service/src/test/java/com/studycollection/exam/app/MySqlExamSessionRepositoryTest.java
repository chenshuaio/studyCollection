package com.studycollection.exam.app;

import com.studycollection.exam.domain.ExamQuestionSnapshot;
import com.studycollection.exam.domain.ExamSession;
import com.studycollection.exam.domain.ExamStatus;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlExamSessionRepositoryTest {
    private MySqlExamSessionRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:exam_sessions;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "sa",
                ""
        );
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("drop table if exists exam_session_answers");
        jdbcTemplate.execute("drop table if exists exam_session_questions");
        jdbcTemplate.execute("drop table if exists exam_sessions");
        jdbcTemplate.execute("""
                create table exam_sessions (
                  id bigint primary key auto_increment,
                  user_id bigint not null,
                  name varchar(128) not null,
                  duration_minutes int not null,
                  status varchar(32) not null,
                  started_at timestamp not null,
                  expires_at timestamp not null,
                  submitted_at timestamp null,
                  score int null,
                  total_score int null,
                  created_at timestamp not null default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table exam_session_questions (
                  id bigint primary key auto_increment,
                  session_id bigint not null,
                  question_id bigint not null,
                  question_title text not null,
                  question_type varchar(32) not null,
                  difficulty varchar(32) not null,
                  knowledge_point varchar(128) not null,
                  correct_answer text not null,
                  analysis text null,
                  sort_order int not null,
                  unique (session_id, question_id)
                )
                """);
        jdbcTemplate.execute("""
                create table exam_session_answers (
                  session_id bigint not null,
                  question_id bigint not null,
                  submitted_answer text not null,
                  auto_graded boolean not null,
                  correct boolean null,
                  score int not null,
                  updated_at timestamp not null default current_timestamp,
                  primary key (session_id, question_id)
                )
                """);
        repository = new MySqlExamSessionRepository(jdbcTemplate);
    }

    @Test
    void roundTripsQuestionSnapshotsAnswersAndCompletion() {
        ExamSession created = repository.create(session());
        repository.save(created.saveAnswer(41L, "A"));

        ExamSession restored = repository.findById(created.id());
        repository.save(restored.complete(Instant.parse("2026-07-11T03:10:00Z")));

        ExamSession completed = repository.findById(created.id());
        assertThat(completed.userId()).isEqualTo(9L);
        assertThat(completed.status()).isEqualTo(ExamStatus.SUBMITTED);
        assertThat(completed.questions()).singleElement().satisfies(question -> {
            assertThat(question.title()).contains("ArrayList");
            assertThat(question.correctAnswer()).isEqualTo("A");
            assertThat(question.analysis()).contains("扩容");
        });
        assertThat(completed.answers().get(41L).submittedAnswer()).isEqualTo("A");
        assertThat(completed.answers().get(41L).correct()).isTrue();
        assertThat(repository.findByUserId(9L)).extracting(ExamSession::id).containsExactly(created.id());
    }

    private ExamSession session() {
        return ExamSession.start(
                9L,
                "集合测试",
                20,
                Instant.parse("2026-07-11T03:00:00Z"),
                List.of(new ExamQuestionSnapshot(
                        41L,
                        "ArrayList 何时扩容？",
                        QuestionType.SINGLE_CHOICE,
                        Difficulty.INTERMEDIATE,
                        "集合框架",
                        "A",
                        "容量不足时触发扩容。",
                        0
                ))
        );
    }
}
