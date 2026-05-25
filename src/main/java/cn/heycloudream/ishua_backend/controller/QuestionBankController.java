package cn.heycloudream.ishua_backend.controller;

import cn.heycloudream.ishua_backend.common.dto.PageRequestDTO;
import cn.heycloudream.ishua_backend.common.vo.PageResultVO;
import cn.heycloudream.ishua_backend.common.vo.Result;
import cn.heycloudream.ishua_backend.annotation.RequireRole;
import cn.heycloudream.ishua_backend.config.OpenApiConfig;
import cn.heycloudream.ishua_backend.config.openapi.ApiDocPublicEndpoint;
import cn.heycloudream.ishua_backend.config.openapi.ApiDocStandardResponses;
import cn.heycloudream.ishua_backend.dto.ai.BatchImportRequestDTO;
import cn.heycloudream.ishua_backend.dto.question.QuestionInBankPageQueryDTO;
import cn.heycloudream.ishua_backend.dto.question.QuestionUpdateDTO;
import cn.heycloudream.ishua_backend.dto.questionbank.QuestionBankCreateDTO;
import cn.heycloudream.ishua_backend.dto.questionbank.QuestionBankUpdateDTO;
import cn.heycloudream.ishua_backend.enums.AiImportTaskStatus;
import cn.heycloudream.ishua_backend.enums.UserRole;
import cn.heycloudream.ishua_backend.exception.BusinessException;
import cn.heycloudream.ishua_backend.service.QuestionBankHotDetailService;
import cn.heycloudream.ishua_backend.service.QuestionBankService;
import cn.heycloudream.ishua_backend.service.QuestionService;
import cn.heycloudream.ishua_backend.service.ai.AiImportResultStore;
import cn.heycloudream.ishua_backend.service.ai.AiImportTaskMetaStore;
import cn.heycloudream.ishua_backend.service.ai.AiImportTaskStatusStore;
import cn.heycloudream.ishua_backend.service.ai.ImportIdempotentService;
import cn.heycloudream.ishua_backend.util.UserContextHolder;
import cn.heycloudream.ishua_backend.vo.ai.AiImportTaskMetaVO;
import cn.heycloudream.ishua_backend.vo.ai.QuestionPreviewVO;
import cn.heycloudream.ishua_backend.vo.question.QuestionVO;
import cn.heycloudream.ishua_backend.vo.questionbank.QuestionBankDetailBundleVO;
import cn.heycloudream.ishua_backend.vo.questionbank.QuestionBankVO;
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
@Tag(name = "题库管理", description = "公开大厅/热点详情无需登录；写操作与「我的题库」须 JWT 且最低 PREMIUM（ADMIN 含）；批量入库见 batch 接口")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiDocStandardResponses
public class QuestionBankController {

    private final QuestionBankService questionBankService;
    private final QuestionService questionService;
    private final QuestionBankHotDetailService questionBankHotDetailService;

    private final ImportIdempotentService importIdempotentService;
    private final AiImportTaskStatusStore taskStatusStore;
    private final AiImportResultStore resultStore;
    private final AiImportTaskMetaStore taskMetaStore;

    @GetMapping
    @RequireRole(UserRole.PREMIUM)
    @Operation(
            summary = "分页查询当前用户的题库列表",
            description = """
                    须 JWT，最低角色 PREMIUM（ADMIN 可）。Query：`current`、`pageSize`（必填）。仅返回当前用户创建的题库，按更新时间倒序。
                    失败：code=401 未登录；code=403 角色为 USER。
                    """)
    public Result<PageResultVO<QuestionBankVO>> pageMyBanks(
            @ParameterObject @Valid @ModelAttribute PageRequestDTO page) {
        Long userId = UserContextHolder.get();
        return Result.success(questionBankService.pageMyBanks(userId, page));
    }

    @GetMapping("/public")
    @ApiDocPublicEndpoint
    @Operation(
            summary = "分页查询所有公开题库列表",
            description = """
                    **无需登录。** Query：`current`、`pageSize`（必填）。
                    仅 is_public=1，按更新时间降序，供刷题大厅展示。
                    """)
    public Result<PageResultVO<QuestionBankVO>> pagePublicBanks(
            @ParameterObject @Valid @ModelAttribute PageRequestDTO page) {
        return Result.success(questionBankService.pagePublicBanks(page));
    }

    @PostMapping
    @RequireRole(UserRole.PREMIUM)
    @Operation(
            summary = "创建题库",
            description = """
                    须 JWT，最低角色 PREMIUM（ADMIN 可）。成功 data 为新题库 ID（Long）。
                    失败：code=401 未登录；code=403 角色为 USER。
                    """)
    public Result<Long> createBank(@Valid @RequestBody QuestionBankCreateDTO dto) {
        Long userId = UserContextHolder.get();
        Long id = questionBankService.createBank(userId, dto);
        return Result.success(id);
    }

    @GetMapping("/{bankId}/hot-practice-detail")
    @ApiDocPublicEndpoint
    @Operation(
            summary = "获取公开热点题库刷题聚合数据（Redis 缓存）",
            description = """
                    **无需登录。** 返回题库信息 + 全量试题（QuestionBankDetailBundleVO）。
                    仅 is_public=1 的公开题库；失败：code=404 不存在，code=403 非公开题库。
                    """)
    public Result<QuestionBankDetailBundleVO> getHotPracticeDetail(
            @Parameter(description = "题库 ID", required = true, example = "1")
            @PathVariable("bankId") Long bankId) {
        return Result.success(questionBankHotDetailService.getHotPublicBankDetail(bankId));
    }

    @GetMapping("/{bankId}/questions")
    @RequireRole(UserRole.PREMIUM)
    @Operation(
            summary = "分页查询指定题库下的试题",
            description = """
                    须 JWT，最低角色 PREMIUM（ADMIN 可 bypass 归属）。须为题库所有者。Query：`current`、`pageSize`（必填），`keyword`（可选，题干模糊）。
                    失败：code=403 角色为 USER；code=404 题库不存在或无权。
                    """)
    public Result<PageResultVO<QuestionVO>> pageQuestionsInBank(
            @Parameter(description = "题库 ID", required = true, example = "1001")
            @PathVariable("bankId") Long bankId,
            @ParameterObject @Valid @ModelAttribute QuestionInBankPageQueryDTO query) {
        Long userId = UserContextHolder.get();
        return Result.success(questionService.pageQuestionsInBank(userId, bankId, query));
    }

    @PostMapping("/{bankId}/questions")
    @RequireRole(UserRole.PREMIUM)
    @Operation(
            summary = "在指定题库下新增试题",
            description = """
                    须 JWT，最低角色 PREMIUM（ADMIN 可 bypass 归属）。请求体为 QuestionUpdateDTO（含 optionsJson、answerJson 等 JSON 字符串字段），
                    **勿传 questionBankId**，题库以路径 bankId 为准。
                    成功 data 为新试题 ID。失败：code=403 角色为 USER；code=404 题库不存在或无权。
                    """)
    public Result<Long> createQuestionInBank(
            @Parameter(description = "题库 ID", required = true, example = "1001")
            @PathVariable("bankId") Long bankId,
            @Valid @RequestBody QuestionUpdateDTO body) {
        Long userId = UserContextHolder.get();
        Long id = questionService.createQuestionInBank(userId, bankId, body);
        return Result.success(id);
    }

    @PostMapping("/{bankId}/questions/batch")
    @RequireRole(UserRole.PREMIUM)
    @Operation(
            summary = "批量确认导入 AI 解析题目（幂等）",
            description = """
                    须 JWT，最低角色 PREMIUM（ADMIN 可 bypass 归属或任务 meta 校验）。前端在 status=PARSED 预览确认后提交 BatchImportRequestDTO。
                    questions 使用 QuestionPreviewVO（options/answer 为数组，与预览接口一致）。

                    - 同一 taskId **仅落库一次**：已导入再次提交 code=200、data=null
                    - 并发落库中：code=409「该任务正在导入中」
                    - taskId 无效/过期：code=400
                    - 角色为 USER：code=403；task 与 bankId/用户不匹配：code=403
                    """)
    public Result<Void> batchImportQuestions(
            @Parameter(description = "题库 ID", required = true, example = "1001")
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
            if (!bankId.equals(meta.getBankId())
                    || (!UserContextHolder.isAdmin() && !userId.equals(meta.getUserId()))) {
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
    @RequireRole(UserRole.PREMIUM)
    @Operation(
            summary = "全量更新题库",
            description = """
                    须 JWT，最低角色 PREMIUM（ADMIN 可 bypass 归属），仅题库所有者可改。成功 data=null。
                    失败：code=403 角色为 USER；code=404 题库不存在或无权。
                    """)
    public Result<Void> updateBank(
            @Parameter(description = "题库 ID", required = true, example = "1001")
            @PathVariable("bankId") Long bankId,
            @Valid @RequestBody QuestionBankUpdateDTO dto) {
        Long userId = UserContextHolder.get();
        questionBankService.updateBank(userId, bankId, dto);
        return Result.success(null);
    }

    @DeleteMapping("/{bankId}")
    @RequireRole(UserRole.PREMIUM)
    @Operation(
            summary = "删除题库",
            description = """
                    须 JWT，最低角色 PREMIUM（ADMIN 可 bypass 归属），逻辑删除题库并级联逻辑删除其下全部试题。
                    失败：code=403 角色为 USER；code=404 题库不存在或无权。
                    """)
    public Result<Void> deleteBank(
            @Parameter(description = "题库 ID", required = true, example = "1001")
            @PathVariable("bankId") Long bankId) {
        Long userId = UserContextHolder.get();
        questionBankService.deleteBank(userId, bankId);
        return Result.success(null);
    }
}
