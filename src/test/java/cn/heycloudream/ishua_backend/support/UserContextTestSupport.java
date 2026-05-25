package cn.heycloudream.ishua_backend.support;

import cn.heycloudream.ishua_backend.enums.UserRole;
import cn.heycloudream.ishua_backend.util.UserContextHolder;

/**
 * {@link UserContextHolder} 测试辅助：在单元/Web 测试中手动注入或清理线程上下文。
 */
public final class UserContextTestSupport {

    private UserContextTestSupport() {
    }

    public static void setUser(Long userId) {
        UserContextHolder.set(userId);
    }

    public static void setUser(Long userId, UserRole role) {
        UserContextHolder.set(userId, role);
    }

    public static void clear() {
        UserContextHolder.clear();
    }
}
