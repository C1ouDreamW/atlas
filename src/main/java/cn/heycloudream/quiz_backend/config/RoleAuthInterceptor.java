package cn.heycloudream.quiz_backend.config;

import cn.heycloudream.quiz_backend.annotation.RequireRole;
import cn.heycloudream.quiz_backend.enums.UserRole;
import cn.heycloudream.quiz_backend.exception.BusinessException;
import cn.heycloudream.quiz_backend.util.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 角色授权拦截器。
 *
 * @author C1ouD
 */
@Component
public class RoleAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole requireRole = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RequireRole.class);
        if (requireRole == null) {
            requireRole = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getBeanType(), RequireRole.class);
        }
        if (requireRole == null) {
            return true;
        }

        UserRole currentRole = UserContextHolder.getRole();
        if (currentRole == null) {
            throw new BusinessException(401, "未登录或用户上下文缺失");
        }
        if (!currentRole.includes(requireRole.value())) {
            throw new BusinessException(403, "权限不足");
        }
        return true;
    }
}
