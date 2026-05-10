package cn.heycloudream.quiz_backend.service.impl;

import cn.heycloudream.quiz_backend.entity.QuestionBank;
import cn.heycloudream.quiz_backend.exception.BusinessException;
import cn.heycloudream.quiz_backend.mapper.QuestionBankMapper;
import cn.heycloudream.quiz_backend.service.AiQuestionImportService;
import cn.heycloudream.quiz_backend.service.ai.AiQuestionImportRedisStatusStore;
import cn.heycloudream.quiz_backend.vo.ai.AiImportStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 智能导入门面：同步严格校验、归属权校验，再委托异步处理器并写 Redis 状态。
 *
 * @author atlas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuestionImportServiceImpl implements AiQuestionImportService {

    private final AiQuestionImportAsyncProcessor asyncProcessor;
    private final QuestionBankMapper questionBankMapper;
    private final AiQuestionImportRedisStatusStore importStatusStore;

    @Override
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
