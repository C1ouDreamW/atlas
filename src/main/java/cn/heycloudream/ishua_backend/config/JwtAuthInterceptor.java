package cn.heycloudream.ishua_backend.config;

import cn.heycloudream.ishua_backend.entity.SysUser;
import cn.heycloudream.ishua_backend.enums.UserRole;
import cn.heycloudream.ishua_backend.exception.BusinessException;
import cn.heycloudream.ishua_backend.mapper.SysUserMapper;
import cn.heycloudream.ishua_backend.util.JwtUtils;
import cn.heycloudream.ishua_backend.util.UserContextHolder;
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
 * 验签后实时加载用户角色并写入 {@link UserContextHolder}；
 * 在 {@link #afterCompletion} 中清除上下文，防止线程池内存泄漏。
 * </p>
 *
 * @author C1ouD
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final SysUserMapper sysUserMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(401, "未提供有效的认证Token");
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        Long userId = jwtUtils.parseUserId(token);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在或已被禁用");
        }
        UserRole role = UserRole.fromDbValue(user.getRole());
        UserContextHolder.set(userId, role);

        log.debug("JWT 鉴权通过 userId={} role={} uri={}", userId, role, request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
