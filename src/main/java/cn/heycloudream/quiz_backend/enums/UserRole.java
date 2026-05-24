package cn.heycloudream.quiz_backend.enums;

import cn.heycloudream.quiz_backend.exception.BusinessException;

/**
 * 用户角色枚举。
 *
 * @author C1ouD
 */
public enum UserRole {

    /**
     * 普通用户：可刷公开题库、维护个人错题本。
     */
    USER(1),

    /**
     * 高级用户：可自建题库、管理试题、使用 AI 导入。
     */
    PREMIUM(2),

    /**
     * 管理员：可访问管理端，并在资源归属校验中用于运维放行。
     */
    ADMIN(3);

    private final int level;

    UserRole(int level) {
        this.level = level;
    }

    /**
     * 当前角色是否包含最低角色要求。
     */
    public boolean includes(UserRole minRole) {
        return minRole != null && this.level >= minRole.level;
    }

    /**
     * 从数据库角色值解析。
     */
    public static UserRole fromDbValue(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        String normalized = value.trim().toUpperCase();
        try {
            return UserRole.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(401, "用户角色非法，请联系管理员");
        }
    }

    /**
     * 解析客户端提交的角色值。
     */
    public static UserRole fromRequestValue(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, "角色不能为空");
        }
        try {
            return UserRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "不支持的角色：" + value);
        }
    }
}
