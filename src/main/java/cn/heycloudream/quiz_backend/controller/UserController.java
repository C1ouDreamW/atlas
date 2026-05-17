package cn.heycloudream.quiz_backend.controller;

import cn.heycloudream.quiz_backend.common.vo.Result;
import cn.heycloudream.quiz_backend.config.openapi.ApiDocPublicEndpoint;
import cn.heycloudream.quiz_backend.config.openapi.ApiDocStandardResponses;
import cn.heycloudream.quiz_backend.dto.user.UserLoginDTO;
import cn.heycloudream.quiz_backend.dto.user.UserRegisterDTO;
import cn.heycloudream.quiz_backend.service.UserService;
import cn.heycloudream.quiz_backend.vo.user.UserLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户鉴权 REST 接口：注册与登录。
 * <p>
 * 路径在白名单中，不经过 JWT 拦截器。
 * </p>
 *
 * @author C1ouD
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户鉴权", description = "用户注册与登录（无需 JWT）")
@ApiDocPublicEndpoint
@ApiDocStandardResponses
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(
            summary = "用户注册",
            description = """
                    **无需登录。** 使用 BCrypt 加密存储密码。
                    成功：code=200，data 为 null。
                    失败：code=409 用户名已存在（含已逻辑删除账号）。
                    """)
    public Result<Void> register(@Valid @RequestBody UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success(null);
    }

    @PostMapping("/login")
    @Operation(
            summary = "用户登录",
            description = """
                    **无需登录。** 校验账号密码，成功返回 JWT 与用户信息。
                    失败：code=401 账号或密码错误。
                    后续请求 Header：`Authorization: Bearer <token>`。
                    """)
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        return Result.success(userService.login(dto));
    }
}
