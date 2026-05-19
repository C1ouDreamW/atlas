-- ============================================
-- 管理员种子账号示例
-- 适用场景：本地/测试环境初始化 ADMIN；生产环境请由内网运维流程创建
-- 默认密码示例：123456（上线前必须替换）
-- ============================================

SET NAMES utf8mb4;

INSERT INTO `sys_user` (`username`, `password_hash`, `nickname`, `role`, `create_time`, `update_time`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '平台管理员', 'ADMIN', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `nickname` = VALUES(`nickname`),
  `role` = 'ADMIN',
  `update_time` = NOW();
