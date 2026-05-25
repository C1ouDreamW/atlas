package cn.heycloudream.ishua_backend.annotation;

import cn.heycloudream.ishua_backend.enums.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色门禁注解。
 *
 * @author C1ouD
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 访问该接口所需的最低角色。
     */
    UserRole value();
}
