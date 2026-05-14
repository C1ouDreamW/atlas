package cn.heycloudream.quiz_backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求 DTO。
 *
 * @author C1ouD
 */
@Data
@Schema(description = "用户注册请求")
public class UserRegisterDTO {

    @Schema(description = "登录账号", example = "zhangsan")
    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 64, message = "账号长度需在3-64之间")
    private String username;

    @Schema(description = "登录密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在6-64之间")
    private String password;

    @Schema(description = "昵称（选填）", example = "张三")
    @Size(max = 64, message = "昵称长度不能超过64")
    private String nickname;
}
