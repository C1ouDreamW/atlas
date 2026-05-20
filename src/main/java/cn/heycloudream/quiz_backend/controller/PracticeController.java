package cn.heycloudream.quiz_backend.controller;

import cn.heycloudream.quiz_backend.common.vo.Result;
import cn.heycloudream.quiz_backend.config.OpenApiConfig;
import cn.heycloudream.quiz_backend.config.openapi.ApiDocStandardResponses;
import cn.heycloudream.quiz_backend.dto.practice.AnswerSubmitDTO;
import cn.heycloudream.quiz_backend.service.PracticeService;
import cn.heycloudream.quiz_backend.util.UserContextHolder;
import cn.heycloudream.quiz_backend.vo.practice.AnswerSubmitResultVO;
import cn.heycloudream.quiz_backend.vo.practice.PracticeQuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 刷题 REST 接口：按题库拉题（顺序/随机）、提交答案判分。
 *
 * @author C1ouD
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/practice")
@RequiredArgsConstructor
@Validated
@Tag(name = "在线刷题", description = "须 JWT，最低角色 USER。公开库任意登录用户可刷；私有库须 PREMIUM+ 且为所有者（ADMIN 可 bypass）")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiDocStandardResponses
public class PracticeController {

    private final PracticeService practiceService;

    @GetMapping("/banks/{bankId}/questions")
    @Operation(
            summary = "获取刷题题目列表（不含答案）",
            description = """
                    须 JWT，最低角色 USER。返回指定题库的全量试题，**不包含答案与解析**，防止用户提交前获取答案。
                    - **公开题库**（is_public=1）：任意登录用户可刷，复用 Redis 热点缓存。
                    - **私有题库**：仅 PREMIUM 且为题库所有者可刷；USER 刷他人或任意私有库 → code=403；ADMIN 可 bypass。
                    - `random=true` 时随机打乱题目顺序。
                    答案与解析在提交接口（`POST .../submit`）后返回。
                    失败：code=404 题库不存在；code=403 私有库无权（含 USER 访问私有库）。
                    """)
    public Result<List<PracticeQuestionVO>> listPracticeQuestions(
            @Parameter(description = "题库 ID", required = true, example = "1001")
            @PathVariable("bankId") Long bankId,
            @Parameter(description = "是否随机打乱题目顺序，默认 false（按 sortNo 顺序）", example = "false")
            @RequestParam(value = "random", defaultValue = "false") boolean random) {
        Long userId = UserContextHolder.get();
        List<PracticeQuestionVO> questions = practiceService.listPracticeQuestions(userId, bankId, random);
        return Result.success(questions);
    }

    @PostMapping("/banks/{bankId}/questions/{questionId}/submit")
    @Operation(
            summary = "提交答案并获取判分结果",
            description = """
                    须 JWT，最低角色 USER。提交指定试题的答案，服务端判分并返回结果。
                    公开/私有题库访问规则同「获取刷题题目列表」。
                    - 客观题（SINGLE / MULTI / JUDGE）：自动判分，`correct` 为 true/false。
                    - 答错时自动将该题加入错题本（若已在错题本中则递增错误次数）。
                    - 主观题或未知题型：`needsManualGrading=true`，`correct=null`，**不自动加入错题本**。
                    - 答案与解析在响应的 `answerJson`、`analysis` 字段中返回。
                    - 用户答案格式：单选/多选传大写字母列表如 `[\"A\"]`、`[\"A\",\"C\"]`；判断题传 `[\"T\"]` 或 `[\"F\"]`。
                    失败：code=404 题库/试题不存在；code=400 试题不属于该题库；code=403 私有库无权（USER 刷私有库或非所有者）。
                    """)
    public Result<AnswerSubmitResultVO> submitAnswer(
            @Parameter(description = "题库 ID", required = true, example = "1001")
            @PathVariable("bankId") Long bankId,
            @Parameter(description = "试题 ID", required = true, example = "50001")
            @PathVariable("questionId") Long questionId,
            @Valid @RequestBody AnswerSubmitDTO dto) {
        Long userId = UserContextHolder.get();
        AnswerSubmitResultVO result = practiceService.submitAnswer(userId, bankId, questionId, dto);
        return Result.success(result);
    }
}
