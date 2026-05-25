package cn.heycloudream.ishua_backend.dto.admin;

import cn.heycloudream.ishua_backend.common.dto.PageRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端用户分页查询 DTO。
 *
 * @author C1ouD
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端用户分页查询请求")
public class AdminUserPageQueryDTO extends PageRequestDTO {

    @Schema(description = "用户名关键字（可选）", example = "zhang")
    @Size(max = 64, message = "用户名关键字长度不能超过64")
    private String username;

    @Schema(description = "角色筛选：USER/PREMIUM/ADMIN（可选）", example = "USER")
    private String role;
}
