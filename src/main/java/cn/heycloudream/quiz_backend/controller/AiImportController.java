package cn.heycloudream.quiz_backend.controller;

import cn.heycloudream.quiz_backend.common.vo.Result;
import cn.heycloudream.quiz_backend.annotation.RequireRole;
import cn.heycloudream.quiz_backend.config.OpenApiConfig;
import cn.heycloudream.quiz_backend.config.openapi.ApiDocStandardResponses;
import cn.heycloudream.quiz_backend.enums.UserRole;
import cn.heycloudream.quiz_backend.exception.BusinessException;
import cn.heycloudream.quiz_backend.service.AiQuestionImportService;
import cn.heycloudream.quiz_backend.service.ai.AiImportRateLimiter;
import cn.heycloudream.quiz_backend.service.ai.AiImportResultStore;
import cn.heycloudream.quiz_backend.service.ai.AiImportTaskMetaStore;
import cn.heycloudream.quiz_backend.service.ai.AiImportTaskStatusStore;
import cn.heycloudream.quiz_backend.util.UserContextHolder;
import cn.heycloudream.quiz_backend.vo.ai.AiImportSubmitVO;
import cn.heycloudream.quiz_backend.vo.ai.AiImportTaskMetaVO;
import cn.heycloudream.quiz_backend.vo.ai.AiImportTaskStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI 智能导入控制器（流程 A 生产者侧 + 状态轮询）。
 * <p>
 * 文档解析与大模型调用由 transf-python Worker 完成，Java 仅负责落盘 / 入 Stream / 提供任务状态。
 * </p>
 *
 * @author atlas
 */
@RestController
@RequestMapping("/api/v1/ai-import")
@RequiredArgsConstructor
@Validated
@Tag(name = "AI 智能导入", description = "文件上传 → Stream 派发 → 状态轮询 → 预览确认入库（须 JWT）")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiDocStandardResponses
@RequireRole(UserRole.PREMIUM)
public class AiImportController {

    private final AiQuestionImportService aiQuestionImportService;
    private final AiImportTaskStatusStore statusStore;
    private final AiImportResultStore resultStore;
    private final AiImportTaskMetaStore metaStore;
    private final AiImportRateLimiter rateLimiter;

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "提交文件导入任务（异步）",
            description = """
                    `multipart/form-data` 字段：
                    - **file**（必填）：.txt / .pdf / .docx，最大 10MB
                    - **bankId**（必填）：目标题库 ID（form 字段；亦可通过 query `?bankId=` 传递，二者等价）

                    成功立即返回 taskId（status=SUBMITTED），后台经 Redis Stream 派发至 Python Worker。
                    轮询：`GET /api/v1/ai-import/tasks/{taskId}/status`。
                    预览确认入库：`POST /api/v1/question-banks/{bankId}/questions/batch`。

                    失败：code=400（文件/格式/大小）、401、404（题库无权）、429（默认每用户每小时 5 次，见配置 quiz.ai-import.rate-limit）。
                    """)
    public Result<AiImportSubmitVO> submitImport(
            @Parameter(
                    description = "导入文件，支持 txt / pdf / docx，最大 10MB",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "目标题库 ID（须为当前用户所属题库）", required = true, example = "1001")
            @RequestParam("bankId") Long bankId) {
        Long userId = UserContextHolder.get();
        rateLimiter.checkAndConsume(userId);
        AiImportSubmitVO vo = aiQuestionImportService.submitFileImport(userId, bankId, file);
        return Result.success(vo);
    }

    @GetMapping("/tasks/{taskId}/status")
    @Operation(
            summary = "轮询 AI 导入任务状态",
            description = """
                    任务状态流转：SUBMITTED → PROCESSING → PARSED →（用户确认 batch）→ IMPORTED；
                    任意环节失败为 FAILED（message 含原因）。

                    - **PARSED**：data.questions 为预览列表（QuestionPreviewVO，options/answer 为数组）
                    - **任务不存在或已过期**：code=200 且 **data=null**（非 HTTP 404）
                    - 仅任务提交者或 ADMIN 可读取任务状态与预览结果

                    建议前端 2~5 秒轮询，终态为 IMPORTED 或 FAILED 后停止。
                    """)
    public Result<AiImportTaskStatusVO> getTaskStatus(
            @Parameter(description = "提交接口返回的任务 ID（UUID）", required = true)
            @PathVariable("taskId") String taskId) {
        Long userId = UserContextHolder.get();
        AiImportTaskMetaVO meta = metaStore.read(taskId).orElse(null);
        if (meta == null) {
            return Result.success(null);
        }
        if (!UserContextHolder.isAdmin() && !userId.equals(meta.getUserId())) {
            throw new BusinessException(403, "无权访问该任务");
        }

        AiImportTaskStatusVO vo = statusStore.read(taskId).orElse(null);
        if (vo == null) {
            return Result.success(null);
        }
        if ("PARSED".equals(vo.getStatus())) {
            resultStore.readQuestions(taskId).ifPresent(vo::setQuestions);
            vo.setTotalCount(vo.getQuestions() != null ? vo.getQuestions().size() : 0);
        }
        return Result.success(vo);
    }
}
