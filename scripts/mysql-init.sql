CREATE DATABASE IF NOT EXISTS study_collection
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE study_collection;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  role VARCHAR(24) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  owner_user_id BIGINT NULL,
  title TEXT NOT NULL,
  type VARCHAR(32) NOT NULL,
  difficulty VARCHAR(32) NOT NULL,
  knowledge_point VARCHAR(128) NOT NULL,
  answer TEXT NOT NULL,
  analysis TEXT NULL,
  source VARCHAR(128) NULL,
  version INT NOT NULL DEFAULT 1,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_questions_filter (knowledge_point, difficulty, type),
  INDEX idx_questions_owner_filter (owner_user_id, knowledge_point, difficulty, type)
);

SET @question_owner_column_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'questions'
    AND column_name = 'owner_user_id'
);
SET @question_owner_ddl = IF(
  @question_owner_column_exists = 0,
  'ALTER TABLE questions ADD COLUMN owner_user_id BIGINT NULL AFTER id',
  'SELECT 1'
);
PREPARE question_owner_statement FROM @question_owner_ddl;
EXECUTE question_owner_statement;
DEALLOCATE PREPARE question_owner_statement;

SET @question_deleted_column_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'questions'
    AND column_name = 'deleted'
);
SET @question_deleted_ddl = IF(
  @question_deleted_column_exists = 0,
  'ALTER TABLE questions ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE AFTER version',
  'SELECT 1'
);
PREPARE question_deleted_statement FROM @question_deleted_ddl;
EXECUTE question_deleted_statement;
DEALLOCATE PREPARE question_deleted_statement;

SET @question_owner_index_exists = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'questions'
    AND index_name = 'idx_questions_owner_filter'
);
SET @question_owner_index_ddl = IF(
  @question_owner_index_exists = 0,
  'CREATE INDEX idx_questions_owner_filter ON questions (owner_user_id, knowledge_point, difficulty, type)',
  'SELECT 1'
);
PREPARE question_owner_index_statement FROM @question_owner_index_ddl;
EXECUTE question_owner_index_statement;
DEALLOCATE PREPARE question_owner_index_statement;

CREATE TABLE IF NOT EXISTS knowledge_points (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL UNIQUE,
  description TEXT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS question_options (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_id BIGINT NOT NULL,
  option_key VARCHAR(8) NOT NULL,
  option_text TEXT NOT NULL,
  FOREIGN KEY (question_id) REFERENCES questions(id)
);

CREATE TABLE IF NOT EXISTS question_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  content TEXT NOT NULL,
  submitted_answer TEXT NOT NULL,
  source_context VARCHAR(32) NOT NULL,
  source_reference VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  reviewed_by BIGINT NULL,
  review_note TEXT NULL,
  reviewed_at DATETIME(6) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_feedback_status (status),
  FOREIGN KEY (question_id) REFERENCES questions(id)
);

CREATE TABLE IF NOT EXISTS question_revisions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_id BIGINT NOT NULL,
  feedback_id BIGINT NULL,
  related_feedback_ids LONGTEXT NOT NULL,
  admin_user_id BIGINT NOT NULL,
  change_summary TEXT NOT NULL,
  review_note TEXT NULL,
  before_snapshot LONGTEXT NOT NULL,
  after_snapshot LONGTEXT NOT NULL,
  scoring_affected BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (question_id) REFERENCES questions(id),
  FOREIGN KEY (feedback_id) REFERENCES question_feedback(id)
);

SET @feedback_columns = CONCAT(
  IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'question_feedback' AND column_name = 'submitted_answer') = 0,
    ', ADD COLUMN submitted_answer TEXT NULL AFTER content', ''),
  IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'question_feedback' AND column_name = 'source_context') = 0,
    ', ADD COLUMN source_context VARCHAR(32) NULL AFTER submitted_answer', ''),
  IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'question_feedback' AND column_name = 'source_reference') = 0,
    ', ADD COLUMN source_reference VARCHAR(128) NULL AFTER source_context', ''),
  IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'question_feedback' AND column_name = 'reviewed_by') = 0,
    ', ADD COLUMN reviewed_by BIGINT NULL AFTER status', ''),
  IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'question_feedback' AND column_name = 'review_note') = 0,
    ', ADD COLUMN review_note TEXT NULL AFTER reviewed_by', ''),
  IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'question_feedback' AND column_name = 'reviewed_at') = 0,
    ', ADD COLUMN reviewed_at DATETIME(6) NULL AFTER review_note', '')
);
SET @feedback_columns_ddl = IF(
  @feedback_columns = '',
  'SELECT 1',
  CONCAT('ALTER TABLE question_feedback ', SUBSTRING(@feedback_columns, 3))
);
PREPARE feedback_columns_statement FROM @feedback_columns_ddl;
EXECUTE feedback_columns_statement;
DEALLOCATE PREPARE feedback_columns_statement;

UPDATE question_feedback
SET submitted_answer = COALESCE(submitted_answer, ''),
    source_context = COALESCE(source_context, 'UNKNOWN'),
    source_reference = COALESCE(source_reference, ''),
    review_note = COALESCE(review_note, '');
ALTER TABLE question_feedback MODIFY submitted_answer TEXT NOT NULL;
ALTER TABLE question_feedback MODIFY source_context VARCHAR(32) NOT NULL;
ALTER TABLE question_feedback MODIFY source_reference VARCHAR(128) NOT NULL;

SET @revision_columns = CONCAT(
  IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'question_revisions' AND column_name = 'related_feedback_ids') = 0,
    ', ADD COLUMN related_feedback_ids LONGTEXT NULL AFTER feedback_id', ''),
  IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'question_revisions' AND column_name = 'before_snapshot') = 0,
    ', ADD COLUMN before_snapshot LONGTEXT NULL AFTER review_note', ''),
  IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'question_revisions' AND column_name = 'after_snapshot') = 0,
    ', ADD COLUMN after_snapshot LONGTEXT NULL AFTER before_snapshot', ''),
  IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'question_revisions' AND column_name = 'scoring_affected') = 0,
    ', ADD COLUMN scoring_affected BOOLEAN NOT NULL DEFAULT FALSE AFTER after_snapshot', '')
);
SET @revision_columns_ddl = IF(
  @revision_columns = '',
  'SELECT 1',
  CONCAT('ALTER TABLE question_revisions ', SUBSTRING(@revision_columns, 3))
);
PREPARE revision_columns_statement FROM @revision_columns_ddl;
EXECUTE revision_columns_statement;
DEALLOCATE PREPARE revision_columns_statement;

UPDATE question_revisions revisions
JOIN questions question ON question.id = revisions.question_id
SET revisions.related_feedback_ids = COALESCE(
      revisions.related_feedback_ids,
      JSON_ARRAY(revisions.feedback_id)
    ),
    revisions.before_snapshot = COALESCE(
      revisions.before_snapshot,
      JSON_OBJECT(
        'id', question.id,
        'title', question.title,
        'type', question.type,
        'difficulty', question.difficulty,
        'knowledgePoint', question.knowledge_point,
        'answer', question.answer,
        'analysis', question.analysis,
        'source', question.source
      )
    ),
    revisions.after_snapshot = COALESCE(
      revisions.after_snapshot,
      JSON_OBJECT(
        'id', question.id,
        'title', question.title,
        'type', question.type,
        'difficulty', question.difficulty,
        'knowledgePoint', question.knowledge_point,
        'answer', question.answer,
        'analysis', question.analysis,
        'source', question.source
      )
    );
ALTER TABLE question_revisions MODIFY related_feedback_ids LONGTEXT NOT NULL;
ALTER TABLE question_revisions MODIFY before_snapshot LONGTEXT NOT NULL;
ALTER TABLE question_revisions MODIFY after_snapshot LONGTEXT NOT NULL;

CREATE TABLE IF NOT EXISTS pending_questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  submitter_user_id BIGINT NOT NULL,
  title TEXT NOT NULL,
  type VARCHAR(32) NOT NULL,
  difficulty VARCHAR(32) NOT NULL,
  knowledge_point VARCHAR(128) NOT NULL,
  answer TEXT NOT NULL,
  analysis TEXT NULL,
  target_scope VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_pending_questions_status (status)
);

SET @pending_target_scope_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'pending_questions'
    AND column_name = 'target_scope'
);
SET @pending_target_scope_ddl = IF(
  @pending_target_scope_exists = 0,
  'ALTER TABLE pending_questions ADD COLUMN target_scope VARCHAR(16) NOT NULL DEFAULT ''PUBLIC'' AFTER analysis',
  'SELECT 1'
);
PREPARE pending_target_scope_statement FROM @pending_target_scope_ddl;
EXECUTE pending_target_scope_statement;
DEALLOCATE PREPARE pending_target_scope_statement;

CREATE TABLE IF NOT EXISTS exam_papers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  duration_minutes INT NOT NULL,
  mode VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exam_paper_questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  sort_order INT NOT NULL,
  FOREIGN KEY (paper_id) REFERENCES exam_papers(id),
  FOREIGN KEY (question_id) REFERENCES questions(id)
);

CREATE TABLE IF NOT EXISTS exam_rules (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  description TEXT NOT NULL,
  duration_minutes INT NOT NULL,
  total_questions INT NOT NULL,
  knowledge_points TEXT NOT NULL,
  type_quotas TEXT NOT NULL,
  difficulty_quotas TEXT NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_exam_rules_status_updated (status, updated_at)
);

CREATE TABLE IF NOT EXISTS exam_sessions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  duration_minutes INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  started_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  submitted_at DATETIME(6) NULL,
  score INT NULL,
  total_score INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_exam_sessions_user_started (user_id, started_at)
);

CREATE TABLE IF NOT EXISTS exam_session_questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  question_title TEXT NOT NULL,
  question_type VARCHAR(32) NOT NULL,
  difficulty VARCHAR(32) NOT NULL,
  knowledge_point VARCHAR(128) NOT NULL,
  correct_answer TEXT NOT NULL,
  analysis TEXT NULL,
  sort_order INT NOT NULL,
  UNIQUE KEY uk_exam_session_question (session_id, question_id),
  FOREIGN KEY (session_id) REFERENCES exam_sessions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS exam_session_answers (
  session_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  submitted_answer TEXT NOT NULL,
  auto_graded BOOLEAN NOT NULL DEFAULT FALSE,
  correct BOOLEAN NULL,
  score INT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (session_id, question_id),
  FOREIGN KEY (session_id) REFERENCES exam_sessions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mistake_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  question_title TEXT NOT NULL,
  question_type VARCHAR(32) NOT NULL,
  knowledge_point VARCHAR(128) NOT NULL,
  last_submitted_answer TEXT NOT NULL,
  source_context VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  wrong_count INT NOT NULL DEFAULT 1,
  first_wrong_at DATETIME(6) NOT NULL,
  last_wrong_at DATETIME(6) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_mistakes_user_status (user_id, status),
  UNIQUE KEY uk_mistakes_user_question (user_id, question_id)
);

SET @mistake_title_column_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'mistake_records'
    AND column_name = 'question_title'
);
SET @mistake_title_ddl = IF(
  @mistake_title_column_exists = 0,
  'ALTER TABLE mistake_records ADD COLUMN question_title TEXT NULL AFTER question_id',
  'SELECT 1'
);
PREPARE mistake_title_statement FROM @mistake_title_ddl;
EXECUTE mistake_title_statement;
DEALLOCATE PREPARE mistake_title_statement;

UPDATE mistake_records
SET question_title = CONCAT('题目 ', question_id)
WHERE question_title IS NULL OR question_title = '';
ALTER TABLE mistake_records MODIFY question_title TEXT NOT NULL;

SET @mistake_type_column_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'mistake_records'
    AND column_name = 'question_type'
);
SET @mistake_type_ddl = IF(
  @mistake_type_column_exists = 0,
  'ALTER TABLE mistake_records ADD COLUMN question_type VARCHAR(32) NULL AFTER question_title',
  'SELECT 1'
);
PREPARE mistake_type_statement FROM @mistake_type_ddl;
EXECUTE mistake_type_statement;
DEALLOCATE PREPARE mistake_type_statement;

SET @mistake_answer_column_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'mistake_records'
    AND column_name = 'last_submitted_answer'
);
SET @mistake_answer_ddl = IF(
  @mistake_answer_column_exists = 0,
  'ALTER TABLE mistake_records ADD COLUMN last_submitted_answer TEXT NULL AFTER knowledge_point',
  'SELECT 1'
);
PREPARE mistake_answer_statement FROM @mistake_answer_ddl;
EXECUTE mistake_answer_statement;
DEALLOCATE PREPARE mistake_answer_statement;

SET @mistake_source_column_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'mistake_records'
    AND column_name = 'source_context'
);
SET @mistake_source_ddl = IF(
  @mistake_source_column_exists = 0,
  'ALTER TABLE mistake_records ADD COLUMN source_context VARCHAR(32) NULL AFTER last_submitted_answer',
  'SELECT 1'
);
PREPARE mistake_source_statement FROM @mistake_source_ddl;
EXECUTE mistake_source_statement;
DEALLOCATE PREPARE mistake_source_statement;

SET @mistake_count_column_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'mistake_records'
    AND column_name = 'wrong_count'
);
SET @mistake_count_ddl = IF(
  @mistake_count_column_exists = 0,
  'ALTER TABLE mistake_records ADD COLUMN wrong_count INT NOT NULL DEFAULT 1 AFTER status',
  'SELECT 1'
);
PREPARE mistake_count_statement FROM @mistake_count_ddl;
EXECUTE mistake_count_statement;
DEALLOCATE PREPARE mistake_count_statement;

SET @mistake_first_time_column_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'mistake_records'
    AND column_name = 'first_wrong_at'
);
SET @mistake_first_time_ddl = IF(
  @mistake_first_time_column_exists = 0,
  'ALTER TABLE mistake_records ADD COLUMN first_wrong_at DATETIME(6) NULL AFTER wrong_count',
  'SELECT 1'
);
PREPARE mistake_first_time_statement FROM @mistake_first_time_ddl;
EXECUTE mistake_first_time_statement;
DEALLOCATE PREPARE mistake_first_time_statement;

SET @mistake_last_time_column_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'mistake_records'
    AND column_name = 'last_wrong_at'
);
SET @mistake_last_time_ddl = IF(
  @mistake_last_time_column_exists = 0,
  'ALTER TABLE mistake_records ADD COLUMN last_wrong_at DATETIME(6) NULL AFTER first_wrong_at',
  'SELECT 1'
);
PREPARE mistake_last_time_statement FROM @mistake_last_time_ddl;
EXECUTE mistake_last_time_statement;
DEALLOCATE PREPARE mistake_last_time_statement;

UPDATE mistake_records mistakes
LEFT JOIN questions question ON question.id = mistakes.question_id
SET mistakes.question_type = COALESCE(mistakes.question_type, question.type, 'SHORT_ANSWER'),
    mistakes.last_submitted_answer = COALESCE(mistakes.last_submitted_answer, ''),
    mistakes.source_context = COALESCE(mistakes.source_context, 'UNKNOWN'),
    mistakes.wrong_count = GREATEST(COALESCE(mistakes.wrong_count, 1), 1),
    mistakes.first_wrong_at = COALESCE(mistakes.first_wrong_at, mistakes.created_at),
    mistakes.last_wrong_at = COALESCE(mistakes.last_wrong_at, mistakes.updated_at, mistakes.created_at);

ALTER TABLE mistake_records MODIFY question_type VARCHAR(32) NOT NULL;
ALTER TABLE mistake_records MODIFY last_submitted_answer TEXT NOT NULL;
ALTER TABLE mistake_records MODIFY source_context VARCHAR(32) NOT NULL;
ALTER TABLE mistake_records MODIFY first_wrong_at DATETIME(6) NOT NULL;
ALTER TABLE mistake_records MODIFY last_wrong_at DATETIME(6) NOT NULL;

DELETE older
FROM mistake_records older
JOIN mistake_records newer
  ON older.user_id = newer.user_id
  AND older.question_id = newer.question_id
  AND older.id < newer.id;

SET @mistake_unique_index_exists = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'mistake_records'
    AND index_name = 'uk_mistakes_user_question'
);
SET @mistake_unique_ddl = IF(
  @mistake_unique_index_exists = 0,
  'ALTER TABLE mistake_records ADD UNIQUE KEY uk_mistakes_user_question (user_id, question_id)',
  'SELECT 1'
);
PREPARE mistake_unique_statement FROM @mistake_unique_ddl;
EXECUTE mistake_unique_statement;
DEALLOCATE PREPARE mistake_unique_statement;

CREATE TABLE IF NOT EXISTS practice_stats (
  user_id BIGINT PRIMARY KEY,
  answered_question_count INT NOT NULL DEFAULT 0,
  graded_question_count INT NOT NULL DEFAULT 0,
  correct_question_count INT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

SET @practice_graded_column_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'practice_stats'
    AND column_name = 'graded_question_count'
);
SET @practice_graded_ddl = IF(
  @practice_graded_column_exists = 0,
  'ALTER TABLE practice_stats ADD COLUMN graded_question_count INT NOT NULL DEFAULT 0 AFTER answered_question_count',
  'SELECT 1'
);
PREPARE practice_graded_statement FROM @practice_graded_ddl;
EXECUTE practice_graded_statement;
DEALLOCATE PREPARE practice_graded_statement;

SET @practice_graded_backfill = IF(
  @practice_graded_column_exists = 0,
  'UPDATE practice_stats SET graded_question_count = answered_question_count',
  'SELECT 1'
);
PREPARE practice_graded_backfill_statement FROM @practice_graded_backfill;
EXECUTE practice_graded_backfill_statement;
DEALLOCATE PREPARE practice_graded_backfill_statement;

CREATE TABLE IF NOT EXISTS learning_attempts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  activity_type VARCHAR(32) NOT NULL,
  reference_id VARCHAR(64) NOT NULL,
  question_id BIGINT NOT NULL,
  question_title TEXT NOT NULL,
  question_type VARCHAR(32) NOT NULL,
  difficulty VARCHAR(32) NOT NULL,
  knowledge_point VARCHAR(128) NOT NULL,
  submitted_answer TEXT NOT NULL,
  auto_graded BOOLEAN NOT NULL DEFAULT FALSE,
  correct BOOLEAN NULL,
  score INT NOT NULL DEFAULT 0,
  attempted_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_learning_attempt (user_id, activity_type, reference_id, question_id),
  INDEX idx_learning_attempt_user_time (user_id, attempted_at),
  INDEX idx_learning_attempt_user_knowledge (user_id, knowledge_point)
);

CREATE TABLE IF NOT EXISTS learning_reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  weakest_knowledge_point VARCHAR(128) NOT NULL,
  recommendation TEXT NOT NULL,
  analysis_source VARCHAR(32) NOT NULL,
  advice_content TEXT NULL,
  details_json LONGTEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

SET @learning_report_advice_column_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'learning_reports'
    AND column_name = 'advice_content'
);
SET @learning_report_advice_ddl = IF(
  @learning_report_advice_column_exists = 0,
  'ALTER TABLE learning_reports ADD COLUMN advice_content TEXT NULL AFTER analysis_source',
  'SELECT 1'
);
PREPARE learning_report_advice_statement FROM @learning_report_advice_ddl;
EXECUTE learning_report_advice_statement;
DEALLOCATE PREPARE learning_report_advice_statement;

SET @learning_report_details_column_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'learning_reports'
    AND column_name = 'details_json'
);
SET @learning_report_details_ddl = IF(
  @learning_report_details_column_exists = 0,
  'ALTER TABLE learning_reports ADD COLUMN details_json LONGTEXT NULL AFTER advice_content',
  'SELECT 1'
);
PREPARE learning_report_details_statement FROM @learning_report_details_ddl;
EXECUTE learning_report_details_statement;
DEALLOCATE PREPARE learning_report_details_statement;

CREATE TABLE IF NOT EXISTS ai_model_settings (
  id BIGINT PRIMARY KEY,
  provider VARCHAR(32) NOT NULL,
  endpoint VARCHAR(512) NOT NULL,
  model_name VARCHAR(128) NOT NULL,
  updated_by BIGINT NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS ai_call_audits (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  purpose VARCHAR(32) NOT NULL,
  provider VARCHAR(32) NOT NULL,
  model_name VARCHAR(128) NOT NULL,
  status VARCHAR(16) NOT NULL,
  failure_reason VARCHAR(240) NULL,
  duration_ms BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_ai_audit_created_at (created_at)
);

INSERT INTO users (username, password_hash, display_name, role)
VALUES
  ('admin', '{plain}admin123', '系统管理员', 'ADMIN'),
  ('user', '{plain}user123', '学习用户', 'USER')
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  role = VALUES(role);

INSERT INTO knowledge_points (name, description, enabled)
VALUES
  ('Java 基础', '变量、类型、流程控制和基础语法', TRUE),
  ('集合框架', 'List、Map、Set 及常见集合实现', TRUE),
  ('面向对象', '封装、继承、多态、接口和抽象类', TRUE),
  ('JVM', '运行时内存、类加载和虚拟机基础', TRUE),
  ('异常处理', 'try/catch/finally 和异常体系', TRUE),
  ('并发编程', '线程、锁、线程池和并发工具', TRUE)
ON DUPLICATE KEY UPDATE
  description = VALUES(description),
  enabled = VALUES(enabled);

UPDATE questions
SET title = 'Java 中 int 默认值是多少？\nA. 0\nB. null\nC. 1\nD. 不确定'
WHERE title = 'Java 中 int 默认值是多少？'
  AND type = 'SINGLE_CHOICE'
  AND answer = 'A';
