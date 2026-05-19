package cn.heycloudream.quiz_backend.controller.admin;

import cn.heycloudream.quiz_backend.annotation.RequireRole;
import cn.heycloudream.quiz_backend.common.vo.PageResultVO;
import cn.heycloudream.quiz_backend.common.vo.Result;
import cn.heycloudream.quiz_backend.config.OpenApiConfig;
import cn.heycloudream.quiz_backend.config.openapi.ApiDocStandardResponses;
import cn.heycloudream.quiz_backend.dto.admin.AdminUserPageQueryDTO;
import cn.heycloudream.quiz_backend.dto.admin.AdminUserRoleUpdateDTO;
import cn.heycloudream.quiz_backend.enums.UserRole;
import cn.heycloudream.quiz_backend.service.UserService;
import cn.heycloudream.quiz_backend.util.UserContextHolder;
import cn.heycloudream.quiz_backend.vo.admin.AdminUserVO;
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
@Tag(name = "管理端用户管理", description = "管理员查询用户与升降级 USER/PREMIUM")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiDocStandardResponses
@RequireRole(UserRole.ADMIN)
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(
            summary = "管理端分页查询用户",
            description = "仅 ADMIN。支持按 username 模糊查询、按 role 精确筛选。")
    public Result<PageResultVO<AdminUserVO>> pageUsers(
            @ParameterObject @Valid @ModelAttribute AdminUserPageQueryDTO query) {
        return Result.success(userService.pageAdminUsers(query));
    }

    @PutMapping("/{userId}/role")
    @Operation(
            summary = "管理端变更用户角色",
            description = "仅 ADMIN。目标角色只允许 USER 或 PREMIUM；禁止设置 ADMIN，禁止修改已有 ADMIN。")
    public Result<Void> updateRole(
            @Parameter(description = "目标用户 ID", required = true, example = "2")
            @PathVariable("userId") Long userId,
            @Valid @RequestBody AdminUserRoleUpdateDTO dto) {
        userService.updateUserRole(UserContextHolder.get(), userId, dto);
        return Result.success(null);
    }
}
