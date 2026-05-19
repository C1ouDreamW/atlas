package cn.heycloudream.quiz_backend.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户信息 VO。
 *
 * @author C1ouD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "当前登录用户信息")
public class UserMeVO {

    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    @Schema(description = "登录账号", example = "zhangsan")
    private String username;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "用户角色：USER-普通用户，PREMIUM-高级用户，ADMIN-管理员", example = "USER")
    private String role;
}
