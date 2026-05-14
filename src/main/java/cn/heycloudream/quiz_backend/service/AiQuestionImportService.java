package cn.heycloudream.quiz_backend.service;

import cn.heycloudream.quiz_backend.vo.ai.AiImportStatusVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 智能题库导入：编排异步解析与落库（流程 A）。
 *
 * @author C1ouD
 */
public interface AiQuestionImportService {

    /**
     * 提交异步导入任务：立即返回，由线程池执行「调模型 → 解析 JSON → 批量落库」。
     * <p>
     * 在 HTTP 线程内完成入参校验与题库归属校验，防止越权写库。
     * </p>
     *
     * @param currentUserId 当前登录用户 ID
     * @param questionBankId 目标题库 ID
     * @param extractedPlainText 已从文档抽取的纯文本
     * @deprecated 请使用新的 Redis Stream 任务体系（Phase C 提供 submitAiImportText）
     */
    @Deprecated
    void scheduleImportFromText(Long currentUserId, Long questionBankId, String extractedPlainText);

    /**
     * 提交文件导入任务：先将文件（.txt / .pdf / .docx）解析为纯文本，再复用的文本导入异步流程。
     * <p>
     * 文件大小上限见 {@link cn.heycloudream.quiz_backend.common.constants.ValidationConstants#FILE_IMPORT_MAX_SIZE_BYTES}。
     * </p>
     *
     * @param currentUserId 当前登录用户 ID
     * @param questionBankId 目标题库 ID
     * @param file 上传的文件
     * @deprecated 请使用 {@code POST /api/v1/ai-import/submit}（Phase C 实现）
     */
    @Deprecated
    void scheduleImportFromFile(Long currentUserId, Long questionBankId, MultipartFile file);

    /**
     * 查询指定题库最近一次智能导入任务状态（Redis，TTL 约 1 小时）。
     * <p>
     * 需校验当前用户对该题库的归属权；无记录时返回 {@code null}。
     * </p>
     *
     * @param currentUserId 当前登录用户 ID
     * @param questionBankId 题库 ID
     * @return 状态快照；Key 不存在或已过期时为 null
     */
    AiImportStatusVO getImportStatus(Long currentUserId, Long questionBankId);
}
