package cn.heycloudream.quiz_backend.service.impl;

import cn.heycloudream.quiz_backend.common.constants.ValidationConstants;
import cn.heycloudream.quiz_backend.entity.QuestionBank;
import cn.heycloudream.quiz_backend.enums.AiImportTaskStatus;
import cn.heycloudream.quiz_backend.exception.BusinessException;
import cn.heycloudream.quiz_backend.mapper.QuestionBankMapper;
import cn.heycloudream.quiz_backend.service.AiQuestionImportService;
import cn.heycloudream.quiz_backend.service.ai.AiImportTaskMetaStore;
import cn.heycloudream.quiz_backend.service.ai.AiImportTaskStatusStore;
import cn.heycloudream.quiz_backend.service.ai.AiQuestionImportRedisStatusStore;
import cn.heycloudream.quiz_backend.service.ai.RedisStreamTaskDispatcher;
import cn.heycloudream.quiz_backend.service.file.FileStorageService;
import cn.heycloudream.quiz_backend.util.DocumentParseUtils;
import cn.heycloudream.quiz_backend.util.TaskIdGenerator;
import cn.heycloudream.quiz_backend.vo.ai.AiImportStatusVO;
import cn.heycloudream.quiz_backend.vo.ai.AiImportSubmitVO;
import cn.heycloudream.quiz_backend.vo.ai.AiImportTaskMetaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 智能导入门面：同步严格校验、归属权校验，再委托异步处理器并写 Redis 状态。
 *
 * @author C1ouD
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuestionImportServiceImpl implements AiQuestionImportService {

    private final AiQuestionImportAsyncProcessor asyncProcessor;
    private final QuestionBankMapper questionBankMapper;
    private final AiQuestionImportRedisStatusStore importStatusStore;

    // Phase C 新增：新任务体系依赖
    private final FileStorageService fileStorageService;
    private final RedisStreamTaskDispatcher taskDispatcher;
    private final AiImportTaskStatusStore taskStatusStore;
    private final AiImportTaskMetaStore taskMetaStore;

    @Override
    @Deprecated
    public void scheduleImportFromText(Long currentUserId, Long questionBankId, String extractedPlainText) {
        if (currentUserId == null) {
            throw new BusinessException(401, "未登录或用户上下文缺失");
        }
        if (questionBankId == null) {
            throw new BusinessException(400, "题库 ID 不能为空");
        }
        if (extractedPlainText == null || extractedPlainText.isBlank()) {
            throw new BusinessException(400, "导入文本不能为空");
        }
        requireOwnedBank(currentUserId, questionBankId);
        log.info("[任务调度成功] 准备导入题库: {}, 文本长度: {}", questionBankId, extractedPlainText.length());
        asyncProcessor.processAsync(questionBankId, extractedPlainText);
    }

    @Override
    @Deprecated
    public void scheduleImportFromFile(Long currentUserId, Long questionBankId, MultipartFile file) {
        if (currentUserId == null) {
            throw new BusinessException(401, "未登录或用户上下文缺失");
        }
        if (questionBankId == null) {
            throw new BusinessException(400, "题库 ID 不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }

        // 文件大小校验（应用层二重防线，Spring 已通过 max-file-size 拦截超大请求）
        if (file.getSize() > ValidationConstants.FILE_IMPORT_MAX_SIZE_BYTES) {
            throw new BusinessException(400, "文件过大，最大支持 10 MB");
        }

        requireOwnedBank(currentUserId, questionBankId);

        String extractedText;
        try {
            extractedText = DocumentParseUtils.extractText(file);
        } catch (BusinessException e) {
            throw e; // 文档解析业务异常直接透传
        } catch (Exception e) {
            log.error("[文件导入] 文档解析未预期异常 bankId={}", questionBankId, e);
            throw new BusinessException(500, "文件解析失败: " + e.getMessage());
        }

        if (extractedText == null || extractedText.isBlank()) {
            throw new BusinessException(400, "解析后的文本内容为空，无法导入");
        }

        // 委托给已有的文本异步导入流程
        this.scheduleImportFromText(currentUserId, questionBankId, extractedText);
    }

    /**
     * 新任务体系：文件提交 → 落盘 → 写元数据 → 入 Stream → 返回 taskId。
     *
     * @param currentUserId 当前用户
     * @param bankId        目标题库
     * @param file          上传文件
     * @return 任务提交响应（taskId + 初始状态）
     */
    public AiImportSubmitVO submitFileImport(Long currentUserId, Long bankId, MultipartFile file) {
        if (currentUserId == null) {
            throw new BusinessException(401, "未登录或用户上下文缺失");
        }
        if (bankId == null) {
            throw new BusinessException(400, "题库 ID 不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        if (file.getSize() > ValidationConstants.FILE_IMPORT_MAX_SIZE_BYTES) {
            throw new BusinessException(400, "文件过大，最大支持 10 MB");
        }
        requireOwnedBank(currentUserId, bankId);

        // 1. 落盘
        String fileUrl;
        try {
            fileUrl = fileStorageService.store(file);
        } catch (IOException e) {
            log.error("[submitFileImport] 文件落盘失败 bankId={}", bankId, e);
            throw new BusinessException(500, "文件存储失败: " + e.getMessage());
        }

        // 2. 生成 taskId + 元数据
        String taskId = TaskIdGenerator.generate();
        String originalFilename = file.getOriginalFilename();
        long now = System.currentTimeMillis();

        AiImportTaskMetaVO meta = AiImportTaskMetaVO.builder()
                .taskId(taskId)
                .userId(currentUserId)
                .bankId(bankId)
                .fileName(originalFilename)
                .fileSize(file.getSize())
                .fileUrl(fileUrl)
                .submittedAt(now)
                .type("file")
                .build();

        // 3. 写元数据 → Redis
        taskMetaStore.write(taskId, meta);

        // 4. 入 Stream
        taskDispatcher.dispatch(meta);

        // 5. 写初始状态
        taskStatusStore.write(taskId, AiImportTaskStatus.SUBMITTED, null, null);

        log.info("[submitFileImport] 任务已提交 taskId={} bankId={} file={}", taskId, bankId, originalFilename);

        return AiImportSubmitVO.builder()
                .taskId(taskId)
                .status(AiImportTaskStatus.SUBMITTED.name())
                .build();
    }

    @Override
    public AiImportStatusVO getImportStatus(Long currentUserId, Long questionBankId) {
        if (currentUserId == null) {
            throw new BusinessException(401, "未登录或用户上下文缺失");
        }
        if (questionBankId == null) {
            throw new BusinessException(400, "题库 ID 不能为空");
        }
        requireOwnedBank(currentUserId, questionBankId);
        return importStatusStore.read(questionBankId).orElse(null);
    }

    private void requireOwnedBank(Long currentUserId, Long bankId) {
        QuestionBank bank = questionBankMapper.selectById(bankId);
        if (bank == null || bank.getUserId() == null || !bank.getUserId().equals(currentUserId)) {
            throw new BusinessException(404, "题库不存在或无权访问");
        }
    }
}
