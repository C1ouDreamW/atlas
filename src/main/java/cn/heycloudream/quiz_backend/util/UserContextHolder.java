package cn.heycloudream.quiz_backend.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 请求级用户上下文，基于 {@link ThreadLocal} 存储当前登录用户的 {@code userId}。
 * <p>
 * 由 {@code JwtAuthInterceptor#preHandle} 写入，在 {@code afterCompletion} 阶段清除，
 * 防止线程池复用导致的内存泄漏与数据串扰。
 * </p>
 *
 * @author C1ouD
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserContextHolder {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 将当前请求的用户 ID 绑定到线程上下文。
     */
    public static void set(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前请求的用户 ID；未经过鉴权拦截器时可能返回 {@code null}。
     */
    public static Long get() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 清除线程上下文，须在请求结束时调用。
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
