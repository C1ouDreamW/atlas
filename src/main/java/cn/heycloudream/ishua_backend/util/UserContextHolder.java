package cn.heycloudream.ishua_backend.util;

import cn.heycloudream.ishua_backend.enums.UserRole;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 请求级用户上下文，基于 {@link ThreadLocal} 存储当前登录用户的身份与角色。
 * <p>
 * 由 {@code JwtAuthInterceptor#preHandle} 写入，在 {@code afterCompletion} 阶段清除，
 * 防止线程池复用导致的内存泄漏与数据串扰。
 * </p>
 *
 * @author C1ouD
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 将当前请求的用户 ID 绑定到线程上下文。
     */
    public static void set(Long userId) {
        CONTEXT_HOLDER.set(UserContext.builder().userId(userId).build());
    }

    /**
     * 将当前请求的用户身份与角色绑定到线程上下文。
     */
    public static void set(Long userId, UserRole role) {
        CONTEXT_HOLDER.set(UserContext.builder().userId(userId).role(role).build());
    }

    /**
     * 获取当前请求的用户 ID；未经过鉴权拦截器时可能返回 {@code null}。
     */
    public static Long get() {
        UserContext context = CONTEXT_HOLDER.get();
        return context == null ? null : context.getUserId();
    }

    /**
     * 获取当前请求上下文。
     */
    public static UserContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 获取当前请求的用户角色。
     */
    public static UserRole getRole() {
        UserContext context = CONTEXT_HOLDER.get();
        return context == null ? null : context.getRole();
    }

    /**
     * 当前用户是否为管理员。
     */
    public static boolean isAdmin() {
        return UserRole.ADMIN.equals(getRole());
    }

    /**
     * 清除线程上下文，须在请求结束时调用。
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
