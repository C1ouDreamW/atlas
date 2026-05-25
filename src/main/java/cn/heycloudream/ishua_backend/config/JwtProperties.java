package cn.heycloudream.ishua_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性，绑定 application.yaml 中 {@code jwt.*} 键。
 *
 * @author C1ouD
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * HMAC-SHA 签名密钥（建议 ≥256 bit，通过环境变量 {@code JWT_SECRET} 注入）。
     */
    private String secret;

    /**
     * Token 有效期（毫秒），默认 86400000（1 天）。
     */
    private long expiration;
}
