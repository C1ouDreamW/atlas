package cn.heycloudream.quiz_backend.service.impl;

import cn.heycloudream.quiz_backend.common.dto.PageRequestDTO;
import cn.heycloudream.quiz_backend.common.vo.PageResultVO;
import cn.heycloudream.quiz_backend.dto.questionbank.QuestionBankCreateDTO;
import cn.heycloudream.quiz_backend.dto.questionbank.QuestionBankUpdateDTO;
import cn.heycloudream.quiz_backend.entity.QuestionBank;
import cn.heycloudream.quiz_backend.mapper.QuestionBankMapper;
import cn.heycloudream.quiz_backend.service.QuestionBankService;
import cn.heycloudream.quiz_backend.service.QuestionService;
import cn.heycloudream.quiz_backend.service.cache.QuestionBankDetailCacheEvictor;
import cn.heycloudream.quiz_backend.service.guard.BankAccessGuard;
import cn.heycloudream.quiz_backend.vo.questionbank.QuestionBankVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题库服务实现。
 *
 * @author C1ouD
 */
@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionBankMapper questionBankMapper;
    private final QuestionService questionService;
    private final QuestionBankDetailCacheEvictor questionBankDetailCacheEvictor;
    private final BankAccessGuard bankAccessGuard;

    @Override
    public PageResultVO<QuestionBankVO> pageMyBanks(Long currentUserId, PageRequestDTO page) {
        Page<QuestionBank> mp = new Page<>(page.getCurrent(), page.getPageSize());
        LambdaQueryWrapper<QuestionBank> w = new LambdaQueryWrapper<QuestionBank>()
                .eq(QuestionBank::getUserId, currentUserId)
                .orderByDesc(QuestionBank::getUpdateTime);
        questionBankMapper.selectPage(mp, w);
        List<QuestionBankVO> records = mp.getRecords().stream()
                .map(this::toVo)
                .collect(Collectors.toList());
        return PageResultVO.<QuestionBankVO>builder()
                .total(mp.getTotal())
                .records(records)
                .build();
    }

    @Override
    public PageResultVO<QuestionBankVO> pagePublicBanks(PageRequestDTO page) {
        Page<QuestionBank> mp = new Page<>(page.getCurrent(), page.getPageSize());
        LambdaQueryWrapper<QuestionBank> w = new LambdaQueryWrapper<QuestionBank>()
                .eq(QuestionBank::getIsPublic, 1)
                .orderByDesc(QuestionBank::getUpdateTime);
        questionBankMapper.selectPage(mp, w);
        List<QuestionBankVO> records = mp.getRecords().stream()
                .map(this::toVo)
                .collect(Collectors.toList());
        return PageResultVO.<QuestionBankVO>builder()
                .total(mp.getTotal())
                .records(records)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBank(Long currentUserId, QuestionBankCreateDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        QuestionBank entity = QuestionBank.builder()
                .userId(currentUserId)
                .title(dto.getTitle().trim())
                .description(dto.getDescription() == null ? null : dto.getDescription().trim())
                .isPublic(dto.getIsPublic())
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();
        questionBankMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBank(Long currentUserId, Long bankId, QuestionBankUpdateDTO dto) {
        QuestionBank bank = bankAccessGuard.requireOwnedBank(currentUserId, bankId);
        bank.setTitle(dto.getTitle().trim());
        bank.setDescription(dto.getDescription() == null ? null : dto.getDescription().trim());
        bank.setIsPublic(dto.getIsPublic());
        bank.setUpdateTime(LocalDateTime.now());
        questionBankMapper.updateById(bank);
        questionBankDetailCacheEvictor.evict(bankId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBank(Long currentUserId, Long bankId) {
        bankAccessGuard.requireOwnedBank(currentUserId, bankId);
        questionService.removeQuestionsByBankId(bankId);
        questionBankMapper.deleteById(bankId);
        questionBankDetailCacheEvictor.evict(bankId);
    }

    private QuestionBankVO toVo(QuestionBank e) {
        return QuestionBankVO.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .title(e.getTitle())
                .description(e.getDescription())
                .isPublic(e.getIsPublic())
                .createTime(e.getCreateTime())
                .updateTime(e.getUpdateTime())
                .build();
    }
}
