-- ============================================
-- 用户角色迁移脚本
-- 适用场景：三级权限升级时将历史角色值迁移到 USER/PREMIUM/ADMIN
-- ============================================

SET NAMES utf8mb4;

-- 历史普通用户角色迁移
UPDATE `sys_user`
SET `role` = 'USER'
WHERE `role` IS NULL OR `role` = '' OR `role` = 'STUDENT';

-- 若历史环境中存在教师/VIP 等高级能力账号，统一迁移为 PREMIUM
UPDATE `sys_user`
SET `role` = 'PREMIUM'
WHERE `role` IN ('TEACHER', 'VIP');

-- ADMIN 不通过批量迁移产生，仅通过运维脚本或内网流程创建
