-- ============================================
-- 首批核心表 DDL (MySQL 8.x)
-- 字符集: utf8mb4
-- ============================================

SET NAMES utf8mb4;

DROP TABLE IF EXISTS `wrong_question`;
DROP TABLE IF EXISTS `question`;
DROP TABLE IF EXISTS `ai_import_task`;
DROP TABLE IF EXISTS `question_bank`;
DROP TABLE IF EXISTS `sys_user`;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
CREATE TABLE `sys_user` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`        VARCHAR(64)     NOT NULL COMMENT '登录账号（唯一）',
  `password_hash`   VARCHAR(255)    NOT NULL COMMENT 'BCrypt 密码密文',
  `nickname`        VARCHAR(64)     DEFAULT NULL COMMENT '昵称',
  `role`            VARCHAR(32)     NOT NULL DEFAULT 'USER' COMMENT '角色权限：USER-普通用户 PREMIUM-高级用户 ADMIN-管理员',
  `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`, `is_deleted`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 2. 题库表
-- ----------------------------
CREATE TABLE `question_bank` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '创建者用户ID',
  `title`           VARCHAR(200)    NOT NULL COMMENT '题库名称',
  `description`     VARCHAR(1000)   DEFAULT NULL COMMENT '题库描述',
  `is_public`       TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否公开（热点题库可配合 Redis）',
  `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_public` (`user_id`, `is_public`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库表';

-- ----------------------------
-- 2.1 AI 导入任务表
-- ----------------------------
CREATE TABLE `ai_import_task` (
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

-- ----------------------------
-- 3. 试题表
-- ----------------------------
CREATE TABLE `question` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `question_bank_id`    BIGINT UNSIGNED NOT NULL COMMENT '所属题库ID',
  `question_type`       VARCHAR(32)     NOT NULL DEFAULT 'SINGLE' COMMENT '题型：SINGLE/MULTI/JUDGE 等',
  `stem`                LONGTEXT        NOT NULL COMMENT '题干（纯文本，可含换行）',
  `options_json`        JSON            DEFAULT NULL COMMENT '选项列表等结构化数据',
  `answer_json`         JSON            DEFAULT NULL COMMENT '标准答案（单选或多选等）',
  `analysis`            LONGTEXT        DEFAULT NULL COMMENT '解析',
  `raw_llm_json`        LONGTEXT        DEFAULT NULL COMMENT '大模型原始/清洗后完整 JSON 备份（可选）',
  `sort_no`             INT             NOT NULL DEFAULT 0 COMMENT '题库内排序号',
  `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_bank_sort` (`question_bank_id`, `sort_no`, `is_deleted`),
  KEY `idx_bank_id` (`question_bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试题表';

-- ----------------------------
-- 4. 错题本表
-- ----------------------------
-- 业务约定（UPDATE 复活，与 UNIQUE(user_id, question_id) 配合）：
-- 1) 首次做错：INSERT 一行，is_deleted=0。
-- 2) 用户从错题本移除：UPDATE 该行 is_deleted=1（逻辑删除，不物理删行）。
-- 3) 同一用户再次做错同一题：UPDATE 已存在行 SET is_deleted=0，并递增 wrong_count、刷新 last_wrong_time（禁止再 INSERT 第二行，否则会触发唯一约束冲突）。
CREATE TABLE `wrong_question` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `question_id`     BIGINT UNSIGNED NOT NULL COMMENT '试题ID',
  `wrong_count`     INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '累计做错次数',
  `last_wrong_time` DATETIME        DEFAULT NULL COMMENT '最近一次做错时间',
  `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
  KEY `idx_user_create` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错题本表';
