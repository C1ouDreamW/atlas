-- AI 导入任务：Worker 实测 MinerU / LLM 耗时（毫秒）
-- 已有库执行本脚本；新库见 ai_import_task.sql / init_core_tables.sql

SET NAMES utf8mb4;

ALTER TABLE `ai_import_task`
  ADD COLUMN `mineru_duration_ms`   INT UNSIGNED DEFAULT NULL COMMENT 'MinerU 解析耗时（Worker 实测，毫秒）' AFTER `expired_at`,
  ADD COLUMN `llm_duration_ms`      INT UNSIGNED DEFAULT NULL COMMENT 'LLM 抽取耗时（Worker 实测，毫秒）' AFTER `mineru_duration_ms`,
  ADD COLUMN `pipeline_duration_ms` INT UNSIGNED DEFAULT NULL COMMENT 'MinerU+LLM 总耗时（毫秒）' AFTER `llm_duration_ms`;
