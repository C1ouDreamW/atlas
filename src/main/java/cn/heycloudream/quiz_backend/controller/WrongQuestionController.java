package cn.heycloudream.quiz_backend.controller;

import cn.heycloudream.quiz_backend.common.vo.PageResultVO;
import cn.heycloudream.quiz_backend.common.vo.Result;
import cn.heycloudream.quiz_backend.config.OpenApiConfig;
import cn.heycloudream.quiz_backend.config.openapi.ApiDocStandardResponses;
import cn.heycloudream.quiz_backend.dto.wrong.WrongQuestionPageQueryDTO;
import cn.heycloudream.quiz_backend.service.WrongQuestionService;
import cn.heycloudream.quiz_backend.util.UserContextHolder;
import cn.heycloudream.quiz_backend.vo.practice.PracticeQuestionVO;
import cn.heycloudream.quiz_backend.vo.wrong.WrongQuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 错题本 REST 接口：查看、移除错题记录，以及按错题本重刷。
 *
 * @author C1ouD
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/wrong-questions")
@RequiredArgsConstructor
@Validated
@Tag(name = "错题本", description = "查看当前用户错题本、移除错题记录、按错题本重刷")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiDocStandardResponses
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    @GetMapping
    @Operation(
            summary = "分页查询当前用户的错题本",
            description = """
                    须 JWT。分页返回当前用户的错题记录，包含题目摘要（不含答案）。
                    - 可选参数 `bankId`：按题库过滤，不传则返回全部错题。
                    - 按最近做错时间倒序排列。
                    返回 `PageResultVO<WrongQuestionVO>`，`total` 为总条数。
                    """)
    public Result<PageResultVO<WrongQuestionVO>> pageWrongQuestions(
            @ParameterObject @Valid @ModelAttribute WrongQuestionPageQueryDTO query) {
        Long userId = UserContextHolder.get();
        return Result.success(wrongQuestionService.pageWrongQuestions(userId, query));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "从错题本移除一条记录",
            description = """
                    须 JWT，且只能操作本人的错题记录（防止越权）。
                    执行逻辑删除（is_deleted=1），再次做错该题时会自动复活。
                    失败：code=404 记录不存在，code=403 无权操作他人记录。
                    """)
    public Result<Void> removeWrongQuestion(
            @Parameter(description = "错题本记录 ID", required = true, example = "1")
            @PathVariable("id") Long id) {
        Long userId = UserContextHolder.get();
        wrongQuestionService.removeWrongQuestion(userId, id);
        return Result.success(null);
    }

    @GetMapping("/practice")
    @Operation(
            summary = "按错题本重刷（获取错题刷题列表）",
            description = """
                    须 JWT。返回当前用户错题本中所有题目的刷题 VO（不含答案与解析）。
                    - 可选参数 `bankId`：只返回指定题库的错题，不传则返回全部错题。
                    - 按最近做错时间倒序排列。
                    前端拿到题目后仍通过 `POST /api/v1/practice/banks/{bankId}/questions/{questionId}/submit` 提交答案。
                    """)
    public Result<List<PracticeQuestionVO>> listWrongPractice(
            @Parameter(description = "按题库 ID 过滤，不传则返回全部错题", example = "1001")
            @RequestParam(value = "bankId", required = false) Long bankId) {
        Long userId = UserContextHolder.get();
        return Result.success(wrongQuestionService.listWrongPractice(userId, bankId));
    }
}
