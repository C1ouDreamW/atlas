package cn.heycloudream.ishua_backend.controller.admin;

import cn.heycloudream.ishua_backend.annotation.RequireRole;
import cn.heycloudream.ishua_backend.common.vo.PageResultVO;
import cn.heycloudream.ishua_backend.common.vo.Result;
import cn.heycloudream.ishua_backend.config.OpenApiConfig;
import cn.heycloudream.ishua_backend.config.openapi.ApiDocStandardResponses;
import cn.heycloudream.ishua_backend.dto.admin.AdminUserPageQueryDTO;
import cn.heycloudream.ishua_backend.dto.admin.AdminUserRoleUpdateDTO;
import cn.heycloudream.ishua_backend.enums.UserRole;
import cn.heycloudream.ishua_backend.service.UserService;
import cn.heycloudream.ishua_backend.util.UserContextHolder;
import cn.heycloudream.ishua_backend.vo.admin.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户权限接口。
 *
 * @author C1ouD
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "管理端用户管理", description = "须 JWT，仅 ADMIN 可访问；USER/PREMIUM 调用返回 code=403")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiDocStandardResponses
@RequireRole(UserRole.ADMIN)
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(
            summary = "管理端分页查询用户",
            description = """
                    须 JWT，仅 ADMIN。支持按 username 模糊查询、按 role（USER/PREMIUM/ADMIN）精确筛选。
                    失败：code=401 未登录；code=403 非管理员。
                    """)
    public Result<PageResultVO<AdminUserVO>> pageUsers(
            @ParameterObject @Valid @ModelAttribute AdminUserPageQueryDTO query) {
        return Result.success(userService.pageAdminUsers(query));
    }

    @PutMapping("/{userId}/role")
    @Operation(
            summary = "管理端变更用户角色",
            description = """
                    须 JWT，仅 ADMIN。目标角色只允许 USER 或 PREMIUM；禁止设置 ADMIN，禁止修改已有 ADMIN。
                    失败：code=401 未登录；code=403 非管理员或违反角色变更规则。
                    """)
    public Result<Void> updateRole(
            @Parameter(description = "目标用户 ID", required = true, example = "2")
            @PathVariable("userId") Long userId,
            @Valid @RequestBody AdminUserRoleUpdateDTO dto) {
        userService.updateUserRole(UserContextHolder.get(), userId, dto);
        return Result.success(null);
    }
}
