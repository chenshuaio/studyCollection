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
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_questions_filter (knowledge_point, difficulty, type)
);

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

CREATE TABLE IF NOT EXISTS mistake_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  knowledge_point VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_mistakes_user_status (user_id, status)
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
  password_hash = VALUES(password_hash),
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
