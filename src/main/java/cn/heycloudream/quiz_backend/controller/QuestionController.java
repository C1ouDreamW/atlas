package cn.heycloudream.quiz_backend.controller;

import cn.heycloudream.quiz_backend.common.vo.Result;
import cn.heycloudream.quiz_backend.annotation.RequireRole;
import cn.heycloudream.quiz_backend.config.OpenApiConfig;
import cn.heycloudream.quiz_backend.config.openapi.ApiDocStandardResponses;
import cn.heycloudream.quiz_backend.dto.question.QuestionUpdateDTO;
import cn.heycloudream.quiz_backend.enums.UserRole;
import cn.heycloudream.quiz_backend.service.QuestionService;
import cn.heycloudream.quiz_backend.util.UserContextHolder;
import cn.heycloudream.quiz_backend.vo.question.QuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 试题 REST 接口：详情查询、全量更新、删除（资源路径以试题为主键）。
 *
 * @author C1ouD
 */
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Validated
@Tag(name = "试题管理", description = "须 JWT，最低角色 PREMIUM（ADMIN 可 bypass 归属）；USER 调用返回 code=403")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiDocStandardResponses
@RequireRole(UserRole.PREMIUM)
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/{id}")
    @Operation(
            summary = "根据试题 ID 获取详情",
            description = """
                    须 JWT，最低角色 PREMIUM；仅所属题库所有者可查看（ADMIN 可 bypass）。
                    失败：code=403 角色为 USER；code=404 试题不存在或无权访问。
                    """)
    public Result<QuestionVO> getById(
            @Parameter(description = "试题主键", required = true, example = "50001")
            @PathVariable("id") Long id) {
        Long userId = UserContextHolder.get();
        return Result.success(questionService.getQuestionById(userId, id));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "全量更新试题",
            description = """
                    须 JWT，最低角色 PREMIUM。不可更换所属题库；仅所属题库所有者可更新（ADMIN 可 bypass）。
                    请求体见 QuestionUpdateDTO（optionsJson/answerJson 为 JSON 数组字符串）。
                    失败：code=403 角色为 USER；code=404 试题不存在或无权。
                    """)
    public Result<Void> update(
            @Parameter(description = "试题主键", required = true, example = "50001")
            @PathVariable("id") Long id,
            @Valid @RequestBody QuestionUpdateDTO dto) {
        Long userId = UserContextHolder.get();
        questionService.updateQuestion(userId, id, dto);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "删除试题",
            description = """
                    须 JWT，最低角色 PREMIUM。逻辑删除；仅所属题库所有者可删除（ADMIN 可 bypass）。
                    失败：code=403 角色为 USER；code=404 试题不存在或无权。
                    """)
    public Result<Void> delete(
            @Parameter(description = "试题主键", required = true, example = "50001")
            @PathVariable("id") Long id) {
        Long userId = UserContextHolder.get();
        questionService.deleteQuestion(userId, id);
        return Result.success(null);
    }
}
