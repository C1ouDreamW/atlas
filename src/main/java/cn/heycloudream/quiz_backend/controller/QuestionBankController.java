package cn.heycloudream.quiz_backend.controller;

import cn.heycloudream.quiz_backend.common.dto.PageRequestDTO;
import cn.heycloudream.quiz_backend.common.vo.PageResultVO;
import cn.heycloudream.quiz_backend.common.vo.Result;
import cn.heycloudream.quiz_backend.dto.question.QuestionInBankPageQueryDTO;
import cn.heycloudream.quiz_backend.dto.question.QuestionUpdateDTO;
import cn.heycloudream.quiz_backend.dto.questionbank.AiQuestionImportTextDTO;
import cn.heycloudream.quiz_backend.dto.questionbank.QuestionBankCreateDTO;
import cn.heycloudream.quiz_backend.dto.questionbank.QuestionBankUpdateDTO;
import cn.heycloudream.quiz_backend.service.AiQuestionImportService;
import cn.heycloudream.quiz_backend.service.QuestionBankHotDetailService;
import cn.heycloudream.quiz_backend.service.QuestionBankService;
import cn.heycloudream.quiz_backend.service.QuestionService;
import cn.heycloudream.quiz_backend.vo.question.QuestionVO;
import cn.heycloudream.quiz_backend.vo.ai.AiImportStatusVO;
import cn.heycloudream.quiz_backend.vo.questionbank.QuestionBankDetailBundleVO;
import cn.heycloudream.quiz_backend.vo.questionbank.QuestionBankVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题库 REST 接口：分页、创建、更新、删除，以及题库下试题的分页与创建。
 *
 * @author atlas
 */
@RestController
@RequestMapping("/api/v1/question-banks")
@RequiredArgsConstructor
@Validated
@Tag(name = "题库管理", description = "题库 CRUD、分页及题库下试题列表与新增")
public class QuestionBankController {

    private final QuestionBankService questionBankService;
    private final QuestionService questionService;
    private final QuestionBankHotDetailService questionBankHotDetailService;
    private final AiQuestionImportService aiQuestionImportService;

    @GetMapping
    @Operation(summary = "分页查询当前用户的题库列表", description = "按更新时间倒序；当前用户 ID 由服务端安全上下文解析（占位）。")
    public Result<PageResultVO<QuestionBankVO>> pageMyBanks(@Valid @ModelAttribute PageRequestDTO page) {
        Long userId = 1L; // TODO: 从 JWT / Security 上下文中获取当前登录用户 ID
        return Result.success(questionBankService.pageMyBanks(userId, page));
    }

    @PostMapping
    @Operation(summary = "创建题库", description = "写入创建者用户 ID（由服务端安全上下文解析，占位）。")
    public Result<Long> createBank(@Valid @RequestBody QuestionBankCreateDTO dto) {
        Long userId = 1L; // TODO: 从 JWT / Security 上下文中获取当前登录用户 ID
        Long id = questionBankService.createBank(userId, dto);
        return Result.success(id);
    }

    @GetMapping("/{bankId}/hot-practice-detail")
    @Operation(
            summary = "获取公开热点题库刷题聚合数据（Redis 缓存）",
            description = "对应 README 流程 B：先读 Redis（Key: smart_quiz:bank_detail:{bankId}），未命中回源 MySQL；仅 isPublic=1 的题库可用。")
    public Result<QuestionBankDetailBundleVO> getHotPracticeDetail(@PathVariable("bankId") Long bankId) {
        return Result.success(questionBankHotDetailService.getHotPublicBankDetail(bankId));
    }

    @PostMapping("/{bankId}/ai-import/text")
    @Operation(
            summary = "提交智能导入（异步）",
            description = "校验归属后立即返回；进度与结果见 GET .../ai-import/status。状态写入 Redis：smart_quiz:import_status:{bankId}，TTL 约 1 小时。")
    public Result<Void> submitAiImportFromText(
            @PathVariable("bankId") Long bankId,
            @Valid @RequestBody AiQuestionImportTextDTO body) {
        Long userId = 1L; // TODO: 从 JWT / Security 上下文中获取当前登录用户 ID
        aiQuestionImportService.scheduleImportFromText(userId, bankId, body.getPlainText());
        return Result.success(null);
    }

    @GetMapping("/{bankId}/ai-import/status")
    @Operation(
            summary = "查询智能导入异步状态",
            description = "读取 Redis 中的最近一次导入状态（PROCESSING/SUCCESS/FAILED）；无记录或已过期时 data 为 null。")
    public Result<AiImportStatusVO> getAiImportStatus(@PathVariable("bankId") Long bankId) {
        Long userId = 1L; // TODO: 从 JWT / Security 上下文中获取当前登录用户 ID
        return Result.success(aiQuestionImportService.getImportStatus(userId, bankId));
    }

    @GetMapping("/{bankId}/questions")
    @Operation(summary = "分页查询指定题库下的试题", description = "路径中的 bankId 与当前用户归属校验一致后方可访问。")
    public Result<PageResultVO<QuestionVO>> pageQuestionsInBank(
            @PathVariable("bankId") Long bankId,
            @Valid @ModelAttribute QuestionInBankPageQueryDTO query) {
        Long userId = 1L; // TODO: 从 JWT / Security 上下文中获取当前登录用户 ID
        return Result.success(questionService.pageQuestionsInBank(userId, bankId, query));
    }

    @PostMapping("/{bankId}/questions")
    @Operation(
            summary = "在指定题库下新增试题",
            description = "请求体字段与试题全量更新 DTO 一致，无需携带题库 ID，题库以路径 bankId 为准。")
    public Result<Long> createQuestionInBank(
            @PathVariable("bankId") Long bankId,
            @Valid @RequestBody QuestionUpdateDTO body) {
        Long userId = 1L; // TODO: 从 JWT / Security 上下文中获取当前登录用户 ID
        Long id = questionService.createQuestionInBank(userId, bankId, body);
        return Result.success(id);
    }

    @PutMapping("/{bankId}")
    @Operation(summary = "全量更新题库", description = "仅题库所有者可以更新。")
    public Result<Void> updateBank(
            @PathVariable("bankId") Long bankId,
            @Valid @RequestBody QuestionBankUpdateDTO dto) {
        Long userId = 1L; // TODO: 从 JWT / Security 上下文中获取当前登录用户 ID
        questionBankService.updateBank(userId, bankId, dto);
        return Result.success(null);
    }

    @DeleteMapping("/{bankId}")
    @Operation(summary = "删除题库", description = "逻辑删除题库，并级联逻辑删除其下全部试题。")
    public Result<Void> deleteBank(@PathVariable("bankId") Long bankId) {
        Long userId = 1L; // TODO: 从 JWT / Security 上下文中获取当前登录用户 ID
        questionBankService.deleteBank(userId, bankId);
        return Result.success(null);
    }
}
