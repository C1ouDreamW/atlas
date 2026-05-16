package cn.heycloudream.quiz_backend.controller;

import cn.heycloudream.quiz_backend.common.dto.PageRequestDTO;
import cn.heycloudream.quiz_backend.common.vo.PageResultVO;
import cn.heycloudream.quiz_backend.common.vo.Result;
import cn.heycloudream.quiz_backend.dto.ai.BatchImportRequestDTO;
import cn.heycloudream.quiz_backend.dto.question.QuestionInBankPageQueryDTO;
import cn.heycloudream.quiz_backend.dto.question.QuestionUpdateDTO;
import cn.heycloudream.quiz_backend.dto.questionbank.QuestionBankCreateDTO;
import cn.heycloudream.quiz_backend.dto.questionbank.QuestionBankUpdateDTO;
import cn.heycloudream.quiz_backend.enums.AiImportTaskStatus;
import cn.heycloudream.quiz_backend.exception.BusinessException;
import cn.heycloudream.quiz_backend.service.QuestionBankHotDetailService;
import cn.heycloudream.quiz_backend.service.QuestionBankService;
import cn.heycloudream.quiz_backend.service.QuestionService;
import cn.heycloudream.quiz_backend.service.ai.AiImportResultStore;
import cn.heycloudream.quiz_backend.service.ai.AiImportTaskMetaStore;
import cn.heycloudream.quiz_backend.service.ai.AiImportTaskStatusStore;
import cn.heycloudream.quiz_backend.service.ai.ImportIdempotentService;
import cn.heycloudream.quiz_backend.util.UserContextHolder;
import cn.heycloudream.quiz_backend.vo.ai.AiImportTaskMetaVO;
import cn.heycloudream.quiz_backend.vo.ai.QuestionPreviewVO;
import cn.heycloudream.quiz_backend.vo.question.QuestionVO;
import cn.heycloudream.quiz_backend.vo.questionbank.QuestionBankDetailBundleVO;
import cn.heycloudream.quiz_backend.vo.questionbank.QuestionBankVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;

/**
 * 题库 REST 接口：题库 CRUD、试题分页/创建，以及批量确认导入。
 *
 * @author C1ouD
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/question-banks")
@RequiredArgsConstructor
@Validated
@Tag(name = "题库管理", description = "题库 CRUD、分页及题库下试题列表与新增")
public class QuestionBankController {

    private final QuestionBankService questionBankService;
    private final QuestionService questionService;
    private final QuestionBankHotDetailService questionBankHotDetailService;

    private final ImportIdempotentService importIdempotentService;
    private final AiImportTaskStatusStore taskStatusStore;
    private final AiImportResultStore resultStore;
    private final AiImportTaskMetaStore taskMetaStore;

    @GetMapping
    @Operation(summary = "分页查询当前用户的题库列表", description = "按更新时间倒序；当前用户 ID 由服务端安全上下文解析。")
    public Result<PageResultVO<QuestionBankVO>> pageMyBanks(@Valid @ModelAttribute PageRequestDTO page) {
        Long userId = UserContextHolder.get();
        return Result.success(questionBankService.pageMyBanks(userId, page));
    }

    @GetMapping("/public")
    @Operation(summary = "分页查询所有公开题库列表", description = "查询 is_public=1 的题库，按更新时间降序，供刷题大厅展示，无需登录。")
    public Result<PageResultVO<QuestionBankVO>> pagePublicBanks(@Valid @ModelAttribute PageRequestDTO page) {
        return Result.success(questionBankService.pagePublicBanks(page));
    }

    @PostMapping
    @Operation(summary = "创建题库")
    public Result<Long> createBank(@Valid @RequestBody QuestionBankCreateDTO dto) {
        Long userId = UserContextHolder.get();
        Long id = questionBankService.createBank(userId, dto);
        return Result.success(id);
    }

    @GetMapping("/{bankId}/hot-practice-detail")
    @Operation(
            summary = "获取公开热点题库刷题聚合数据（Redis 缓存）",
            description = "对应流程 B：先读 Redis（Key: smart_quiz:bank_detail:{bankId}），未命中回源 MySQL；仅 isPublic=1 的题库可用。")
    public Result<QuestionBankDetailBundleVO> getHotPracticeDetail(@PathVariable("bankId") Long bankId) {
        return Result.success(questionBankHotDetailService.getHotPublicBankDetail(bankId));
    }

    @GetMapping("/{bankId}/questions")
    @Operation(summary = "分页查询指定题库下的试题", description = "路径中的 bankId 与当前用户归属校验一致后方可访问。")
    public Result<PageResultVO<QuestionVO>> pageQuestionsInBank(
            @PathVariable("bankId") Long bankId,
            @Valid @ModelAttribute QuestionInBankPageQueryDTO query) {
        Long userId = UserContextHolder.get();
        return Result.success(questionService.pageQuestionsInBank(userId, bankId, query));
    }

    @PostMapping("/{bankId}/questions")
    @Operation(
            summary = "在指定题库下新增试题",
            description = "请求体字段与试题全量更新 DTO 一致，无需携带题库 ID，题库以路径 bankId 为准。")
    public Result<Long> createQuestionInBank(
            @PathVariable("bankId") Long bankId,
            @Valid @RequestBody QuestionUpdateDTO body) {
        Long userId = UserContextHolder.get();
        Long id = questionService.createQuestionInBank(userId, bankId, body);
        return Result.success(id);
    }

    @PostMapping("/{bankId}/questions/batch")
    @Operation(
            summary = "批量确认导入 AI 解析题目（幂等）",
            description = "前端预览确认后提交。同一 taskId 只落库一次，重复提交直接返回成功。落库完成后清理 Redis 预览缓存。")
    public Result<Void> batchImportQuestions(
            @PathVariable("bankId") Long bankId,
            @Valid @RequestBody BatchImportRequestDTO body) {
        Long userId = UserContextHolder.get();
        String taskId = body.getTaskId();

        // 已成功导入：直接幂等返回
        if (importIdempotentService.isAlreadyImported(taskId)) {
            return Result.success(null);
        }
        // 抢占落库锁；失败说明另一并发请求正在落库，提示前端稍后查询
        if (!importIdempotentService.tryAcquire(taskId)) {
            throw new BusinessException(409, "该任务正在导入中，请稍候再试");
        }

        boolean dbImported = false;
        try {
            AiImportTaskMetaVO meta = taskMetaStore.read(taskId)
                    .orElseThrow(() -> new BusinessException(400, "任务不存在或已过期"));
            if (!userId.equals(meta.getUserId()) || !bankId.equals(meta.getBankId())) {
                throw new BusinessException(403, "任务与题库不匹配或无权操作");
            }

            List<QuestionPreviewVO> previews = resolveImportPreviews(taskId, body.getQuestions());
            questionService.batchImportPreview(userId, bankId, previews);
            dbImported = true;

            // 落库成功 → 立即标记 imported（长 TTL，覆盖原有锁），即使后续步骤抛错也不会误删终态标记
            importIdempotentService.markImported(taskId);
            try {
                taskStatusStore.write(taskId, AiImportTaskStatus.IMPORTED,
                        "已导入 " + previews.size() + " 道题",
                        previews.size());
                resultStore.delete(taskId);
            } catch (Exception postCommitErr) {
                // 数据已落库且 imported 终态已写入，仅日志告警，不再向上抛
                log.warn("[batchImport] 落库成功但状态/缓存清理失败 taskId={}", taskId, postCommitErr);
            }
            return Result.success(null);
        } catch (RuntimeException e) {
            if (!dbImported) {
                importIdempotentService.release(taskId);
            }
            throw e;
        }
    }

    /**
     * 解析待落库题目：优先使用 Redis 预览缓存（与 status 接口一致）。
     * 若请求体题目数明显多于缓存（常见于轮询合并重复），回退为缓存列表。
     */
    private List<QuestionPreviewVO> resolveImportPreviews(String taskId, List<QuestionPreviewVO> fromBody) {
        return resultStore.readQuestions(taskId)
                .filter(cache -> !cache.isEmpty())
                .map(cache -> {
                    if (fromBody.size() > cache.size()) {
                        log.warn("[batchImport] taskId={} 请求体题目数 {} 大于 Redis 缓存 {}，以缓存为准防重复落库",
                                taskId, fromBody.size(), cache.size());
                        return cache;
                    }
                    return fromBody;
                })
                .orElse(fromBody);
    }

    @PutMapping("/{bankId}")
    @Operation(summary = "全量更新题库", description = "仅题库所有者可以更新。")
    public Result<Void> updateBank(
            @PathVariable("bankId") Long bankId,
            @Valid @RequestBody QuestionBankUpdateDTO dto) {
        Long userId = UserContextHolder.get();
        questionBankService.updateBank(userId, bankId, dto);
        return Result.success(null);
    }

    @DeleteMapping("/{bankId}")
    @Operation(summary = "删除题库", description = "逻辑删除题库，并级联逻辑删除其下全部试题。")
    public Result<Void> deleteBank(@PathVariable("bankId") Long bankId) {
        Long userId = UserContextHolder.get();
        questionBankService.deleteBank(userId, bankId);
        return Result.success(null);
    }
}
