package cn.heycloudream.ishua_backend.dto.user;

import cn.heycloudream.ishua_backend.common.constants.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * User registration request.
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

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = ValidationConstants.AUTH_EMAIL_MAX, message = "邮箱长度不能超过254")
    private String email;

    @Schema(description = "邮箱验证码", example = "123456")
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "\\d{" + ValidationConstants.AUTH_EMAIL_CODE_LENGTH + "}", message = "验证码必须是6位数字")
    private String code;

    @Schema(description = "昵称（选填）", example = "张三")
    @Size(max = 64, message = "昵称长度不能超过64")
    private String nickname;
}
