-- ============================================
-- AI 导入任务持久化表
-- MySQL 8.x / utf8mb4
-- ============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_import_task` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `task_id`           VARCHAR(64)     NOT NULL COMMENT '业务任务 ID（UUID，与 Redis key 一致）',
  `user_id`           BIGINT UNSIGNED NOT NULL COMMENT '提交用户',
  `bank_id`           BIGINT UNSIGNED NOT NULL COMMENT '目标题库',
  `status`            VARCHAR(32)     NOT NULL COMMENT 'SUBMITTED/PROCESSING/PARSED/IMPORTING/IMPORTED/FAILED/EXPIRED',
  `file_name`         VARCHAR(255)    DEFAULT NULL COMMENT '原始文件名',
  `file_size`         BIGINT UNSIGNED DEFAULT NULL COMMENT '字节',
  `file_url`          VARCHAR(512)    DEFAULT NULL COMMENT '落盘路径或 URL',
  `import_type`       VARCHAR(16)     NOT NULL DEFAULT 'file' COMMENT 'file/text',
  `question_count`    INT UNSIGNED    DEFAULT NULL COMMENT '解析出的题目数（PARSED 后）',
  `preview_json`      LONGTEXT        DEFAULT NULL COMMENT 'PARSED 预览题列表 JSON（QuestionPreviewVO[]）',
  `error_message`     VARCHAR(500)    DEFAULT NULL COMMENT 'FAILED/EXPIRED 原因摘要',
  `submitted_at`      DATETIME        NOT NULL COMMENT '提交时间',
  `parsed_at`         DATETIME        DEFAULT NULL COMMENT '进入 PARSED 时间',
  `imported_at`       DATETIME        DEFAULT NULL COMMENT '落库完成时间',
  `expired_at`        DATETIME        DEFAULT NULL COMMENT '标记 EXPIRED 时间',
  `mineru_duration_ms`   INT UNSIGNED DEFAULT NULL COMMENT 'MinerU 解析耗时（Worker 实测，毫秒）',
  `llm_duration_ms`      INT UNSIGNED DEFAULT NULL COMMENT 'LLM 抽取耗时（Worker 实测，毫秒）',
  `pipeline_duration_ms` INT UNSIGNED DEFAULT NULL COMMENT 'MinerU+LLM 总耗时（毫秒）',
  `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`, `is_deleted`),
  KEY `idx_user_status_time` (`user_id`, `status`, `submitted_at`, `is_deleted`),
  KEY `idx_bank_status_time` (`bank_id`, `status`, `submitted_at`, `is_deleted`),
  KEY `idx_parsed_expire` (`status`, `parsed_at`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 智能导入任务表';
