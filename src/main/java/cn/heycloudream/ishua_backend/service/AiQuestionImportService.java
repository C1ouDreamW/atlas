package cn.heycloudream.ishua_backend.service;

import cn.heycloudream.ishua_backend.vo.ai.AiImportSubmitVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 智能题库导入：流程 A 生产者侧入口。
 * <p>
 * Java 仅负责文件落盘 + 写元数据 + 入 Redis Stream，
 * 文档解析与大模型调用全部由 transf-python Worker 完成。
 * </p>
 *
 * @author C1ouD
 */
public interface AiQuestionImportService {

    /**
     * 提交文件导入任务：落盘 → 写元数据 → 入 Stream → 返回 taskId。
     *
     * @param currentUserId 当前登录用户 ID
     * @param bankId        目标题库 ID
     * @param file          上传的文件（.txt / .pdf / .docx）
     * @return 任务提交响应
     */
    AiImportSubmitVO submitFileImport(Long currentUserId, Long bankId, MultipartFile file);
}
