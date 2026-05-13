package cn.heycloudream.quiz_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 安全相关 Bean 注册（仅引入 BCrypt，不激活 Spring Security 自动配置链）。
 *
 * @author atlas
 */
@Configuration
public class SecurityConfig {

    /**
     * BCrypt 密码编码器，用于注册时 hash 与登录时比对。
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
