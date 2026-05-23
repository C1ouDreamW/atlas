package cn.heycloudream.quiz_backend.service;

import cn.heycloudream.quiz_backend.dto.questionbank.QuestionBankUpdateDTO;
import cn.heycloudream.quiz_backend.entity.QuestionBank;
import cn.heycloudream.quiz_backend.exception.BusinessException;
import cn.heycloudream.quiz_backend.mapper.QuestionBankMapper;
import cn.heycloudream.quiz_backend.service.cache.QuestionBankDetailCacheEvictor;
import cn.heycloudream.quiz_backend.service.guard.BankAccessGuard;
import cn.heycloudream.quiz_backend.service.impl.QuestionBankServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link QuestionBankServiceImpl} 写操作与缓存驱逐单元测试。
 */
@ExtendWith(MockitoExtension.class)
class QuestionBankServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long BANK_ID = 10L;

    @Mock
    private QuestionBankMapper questionBankMapper;

    @Mock
    private QuestionService questionService;

    @Mock
    private QuestionBankDetailCacheEvictor questionBankDetailCacheEvictor;

    @Mock
    private BankAccessGuard bankAccessGuard;

    @InjectMocks
    private QuestionBankServiceImpl questionBankService;

    @Test
    @DisplayName("updateBank: 成功后驱逐热点缓存")
    void updateBank_success_shouldEvictCache() {
        QuestionBank bank = QuestionBank.builder().id(BANK_ID).userId(USER_ID).title("旧标题").build();
        when(bankAccessGuard.requireOwnedBank(USER_ID, BANK_ID)).thenReturn(bank);

        QuestionBankUpdateDTO dto = new QuestionBankUpdateDTO();
        dto.setTitle("新标题");
        dto.setDescription("描述");
        dto.setIsPublic(1);

        questionBankService.updateBank(USER_ID, BANK_ID, dto);

        verify(questionBankMapper).updateById(bank);
        verify(questionBankDetailCacheEvictor).evict(BANK_ID);
    }

    @Test
    @DisplayName("deleteBank: 成功后驱逐热点缓存")
    void deleteBank_success_shouldEvictCache() {
        when(bankAccessGuard.requireOwnedBank(USER_ID, BANK_ID))
                .thenReturn(QuestionBank.builder().id(BANK_ID).build());

        questionBankService.deleteBank(USER_ID, BANK_ID);

        verify(questionService).removeQuestionsByBankId(BANK_ID);
        verify(questionBankMapper).deleteById(BANK_ID);
        verify(questionBankDetailCacheEvictor).evict(BANK_ID);
    }

    @Test
    @DisplayName("updateBank: 非所有者 → 404")
    void updateBank_notOwner_shouldPropagate404() {
        doThrow(new BusinessException(404, "题库不存在或无权访问"))
                .when(bankAccessGuard).requireOwnedBank(USER_ID, BANK_ID);

        QuestionBankUpdateDTO dto = new QuestionBankUpdateDTO();
        dto.setTitle("x");
        dto.setIsPublic(0);

        assertThatThrownBy(() -> questionBankService.updateBank(USER_ID, BANK_ID, dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(404);
        verify(questionBankDetailCacheEvictor, org.mockito.Mockito.never()).evict(BANK_ID);
    }
}
