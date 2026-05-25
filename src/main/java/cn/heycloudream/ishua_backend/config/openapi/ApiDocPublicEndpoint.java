package cn.heycloudream.ishua_backend.config.openapi;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记无需 JWT 的公开接口（覆盖类上的 {@link io.swagger.v3.oas.annotations.security.SecurityRequirement}）。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SecurityRequirements
public @interface ApiDocPublicEndpoint {
}
