package cn.heycloudream.quiz_backend.support;

import cn.heycloudream.quiz_backend.config.JwtProperties;
import cn.heycloudream.quiz_backend.util.JwtUtils;

/**
 * 测试环境 JWT 签发工具，密钥与 {@code application-test.yml} 中 {@code jwt.secret} 保持一致。
 */
public final class JwtTestHelper {

    /**
     * 与 application-test.yml 中 jwt.secret 相同，供无 Spring 上下文场景使用。
     */
    public static final String TEST_JWT_SECRET = "atlas-test-jwt-secret-key-at-least-256-bits-long!!";

    public static final long DEFAULT_TEST_USER_ID = 1L;

    private static final JwtUtils JWT_UTILS = createJwtUtils();

    private JwtTestHelper() {
    }

    public static JwtUtils jwtUtils() {
        return JWT_UTILS;
    }

    public static String generateToken(Long userId) {
        return JWT_UTILS.generateToken(userId);
    }

    public static String bearerAuthorization(Long userId) {
        return "Bearer " + generateToken(userId);
    }

    public static String bearerAuthorization() {
        return bearerAuthorization(DEFAULT_TEST_USER_ID);
    }

    private static JwtUtils createJwtUtils() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(TEST_JWT_SECRET);
        properties.setExpiration(86_400_000L);
        return new JwtUtils(properties);
    }
}
