package cn.heycloudream.quiz_backend.controller;

import cn.heycloudream.quiz_backend.common.vo.Result;
import cn.heycloudream.quiz_backend.service.AiQuestionImportService;
import cn.heycloudream.quiz_backend.service.ai.AiImportRateLimiter;
import cn.heycloudream.quiz_backend.service.ai.AiImportResultStore;
import cn.heycloudream.quiz_backend.service.ai.AiImportTaskStatusStore;
import cn.heycloudream.quiz_backend.util.UserContextHolder;
import cn.heycloudream.quiz_backend.vo.ai.AiImportSubmitVO;
import cn.heycloudream.quiz_backend.vo.ai.AiImportTaskStatusVO;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "AI 智能导入", description = "文件上传 → Stream 派发 → 状态轮询 → 预览确认")
public class AiImportController {

    private final AiQuestionImportService aiQuestionImportService;
    private final AiImportTaskStatusStore statusStore;
    private final AiImportResultStore resultStore;
    private final AiImportRateLimiter rateLimiter;

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "提交文件导入任务（异步）",
            description = "上传 .txt/.pdf/.docx 文件后立即返回 taskId，后台经 Stream 派发至 Python Worker 解析。"
                    + "轮询状态见 GET /tasks/{taskId}/status。")
    public Result<AiImportSubmitVO> submitImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bankId") Long bankId) {
        Long userId = UserContextHolder.get();
        rateLimiter.checkAndConsume(userId);
        AiImportSubmitVO vo = aiQuestionImportService.submitFileImport(userId, bankId, file);
        return Result.success(vo);
    }

    @GetMapping("/tasks/{taskId}/status")
    @Operation(
            summary = "轮询 AI 导入任务状态",
            description = "返回任务当前状态。当 status=PARSED 时，响应中自动附带解析出的题目预览列表（questions 字段）。")
    public Result<AiImportTaskStatusVO> getTaskStatus(@PathVariable("taskId") String taskId) {
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
