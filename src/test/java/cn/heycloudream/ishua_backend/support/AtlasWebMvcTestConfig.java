package cn.heycloudream.ishua_backend.support;

import cn.heycloudream.ishua_backend.config.JwtAuthInterceptor;
import cn.heycloudream.ishua_backend.config.JwtProperties;
import cn.heycloudream.ishua_backend.config.RoleAuthInterceptor;
import cn.heycloudream.ishua_backend.config.WebMvcConfig;
import cn.heycloudream.ishua_backend.exception.GlobalExceptionHandler;
import cn.heycloudream.ishua_backend.util.JwtUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * {@code @WebMvcTest} 常用切片配置：JWT 拦截器、角色拦截器、全局异常与 JWT 工具。
 * <p>
 * 用法示例：
 * </p>
 * <pre>
 * {@code @WebMvcTest(controllers = PracticeController.class)}
 * 测试类放在 {@code cn.heycloudream.ishua_test.controller}，并声明：
 * {@code @ContextConfiguration(classes = {IShuaTestApplication.class, XxxController.class})}
 * {@code @Import(AtlasWebMvcTestConfig.class)}
 * {@code @ActiveProfiles("test")}
 * </pre>
 * 需对 {@code SysUserMapper} 等依赖使用 {@code @MockBean}，并在带鉴权接口请求上使用
 * {@link MockMvcTestSupport#withBearerAuth}。
 */
@Import({
        WebMvcConfig.class,
        JwtAuthInterceptor.class,
        RoleAuthInterceptor.class,
        JwtUtils.class,
        GlobalExceptionHandler.class
})
@EnableConfigurationProperties(JwtProperties.class)
public class AtlasWebMvcTestConfig {
}
