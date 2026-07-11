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
  INDEX idx_questions_filter (knowledge_point, difficulty, type)
);

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
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_feedback_status (status),
  FOREIGN KEY (question_id) REFERENCES questions(id)
);

CREATE TABLE IF NOT EXISTS question_revisions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_id BIGINT NOT NULL,
  feedback_id BIGINT NULL,
  admin_user_id BIGINT NOT NULL,
  change_summary TEXT NOT NULL,
  review_note TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (question_id) REFERENCES questions(id),
  FOREIGN KEY (feedback_id) REFERENCES question_feedback(id)
);

CREATE TABLE IF NOT EXISTS pending_questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  submitter_user_id BIGINT NOT NULL,
  title TEXT NOT NULL,
  type VARCHAR(32) NOT NULL,
  difficulty VARCHAR(32) NOT NULL,
  knowledge_point VARCHAR(128) NOT NULL,
  answer TEXT NOT NULL,
  analysis TEXT NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_pending_questions_status (status)
);

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
  knowledge_point VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
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
  correct_question_count INT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS learning_reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  weakest_knowledge_point VARCHAR(128) NOT NULL,
  recommendation TEXT NOT NULL,
  analysis_source VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
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
