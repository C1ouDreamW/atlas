package cn.heycloudream.ishua_backend.util;

import cn.heycloudream.ishua_backend.config.JwtProperties;
import cn.heycloudream.ishua_backend.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token 工具：签发与验签/解析。
 * <p>
 * Token 的 {@code sub} 字段存储 {@code userId} 的字符串形式，
 * 验签失败或过期时统一抛出 {@link BusinessException}(401)。
 * </p>
 *
 * @author C1ouD
 */
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtProperties jwtProperties;

    /**
     * 从配置的 secret 派生 HMAC-SHA 密钥。
     */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 JWT Token。
     *
     * @param userId 当前登录用户 ID
     * @return 签名的 JWT 字符串
     */
    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getExpiration());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getKey())
                .compact();
    }

    /**
     * 验签并解析 Token，返回 {@code userId}。
     *
     * @param token JWT Token 字符串（不含 Bearer 前缀）
     * @return 用户 ID
     * @throws BusinessException 401 — Token 过期或无效
     */
    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (ExpiredJwtException e) {
            throw new BusinessException(401, "Token已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(401, "Token无效或已过期");
        }
    }
}
