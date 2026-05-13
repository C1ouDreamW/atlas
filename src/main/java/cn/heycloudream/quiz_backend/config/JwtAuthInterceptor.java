package cn.heycloudream.quiz_backend.config;

import cn.heycloudream.quiz_backend.exception.BusinessException;
import cn.heycloudream.quiz_backend.util.JwtUtils;
import cn.heycloudream.quiz_backend.util.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器。
 * <p>
 * 在 {@link #preHandle} 中从 {@code Authorization: Bearer <token>} 提取 Token，
 * 验签后将 {@code userId} 写入 {@link UserContextHolder}；
 * 在 {@link #afterCompletion} 中清除上下文，防止线程池内存泄漏。
 * </p>
 *
 * @author atlas
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(401, "未提供有效的认证Token");
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        Long userId = jwtUtils.parseUserId(token);
        UserContextHolder.set(userId);

        log.debug("JWT 鉴权通过 userId={} uri={}", userId, request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
