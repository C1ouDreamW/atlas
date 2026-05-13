package cn.heycloudream.quiz_backend.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应 VO。
 *
 * @author atlas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录响应")
public class UserLoginVO {

    @Schema(description = "JWT 访问令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    @Schema(description = "登录账号", example = "zhangsan")
    private String username;

    @Schema(description = "昵称", example = "张三")
    private String nickname;
}
