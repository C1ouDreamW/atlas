package cn.heycloudream.ishua_backend.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端用户角色更新 DTO。
 *
 * @author C1ouD
 */
@Data
@Schema(description = "管理端用户角色更新请求")
public class AdminUserRoleUpdateDTO {

    @NotBlank(message = "角色不能为空")
    @Schema(description = "目标角色，仅允许 USER 或 PREMIUM", example = "PREMIUM")
    private String role;
}
