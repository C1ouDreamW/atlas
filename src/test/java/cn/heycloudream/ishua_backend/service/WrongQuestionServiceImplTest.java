package cn.heycloudream.ishua_backend.service;

import cn.heycloudream.ishua_backend.dto.wrong.WrongQuestionPageQueryDTO;
import cn.heycloudream.ishua_backend.entity.Question;
import cn.heycloudream.ishua_backend.entity.WrongQuestion;
import cn.heycloudream.ishua_backend.exception.BusinessException;
import cn.heycloudream.ishua_backend.mapper.QuestionMapper;
import cn.heycloudream.ishua_backend.mapper.WrongQuestionMapper;
import cn.heycloudream.ishua_backend.service.impl.WrongQuestionServiceImpl;
import cn.heycloudream.ishua_backend.vo.practice.PracticeQuestionVO;
import cn.heycloudream.ishua_backend.vo.wrong.WrongQuestionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WrongQuestionServiceImpl} 核心业务路径单元测试（纯 Mockito，无 Spring 上下文）。
 */
@ExtendWith(MockitoExtension.class)
class WrongQuestionServiceImplTest {

    @Mock
    private WrongQuestionMapper wrongQuestionMapper;

    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private WrongQuestionServiceImpl wrongQuestionService;

    private static final Long USER_ID = 1L;
    private static final Long QUESTION_ID = 100L;
    private static final Long BANK_ID = 10L;

    private Question sampleQuestion() {
        return Question.builder()
                .id(QUESTION_ID)
                .questionBankId(BANK_ID)
                .questionType("SINGLE")
                .stem("示例题干")
                .optionsJson("[\"A\",\"B\",\"C\"]")
                .answerJson("[\"A\"]")
                .sortNo(1)
                .isDeleted(0)
                .build();
    }

    // -------- recordWrong --------

    @Test
    @DisplayName("recordWrong: 首次做错 → 执行 INSERT")
    void recordWrong_firstTime_shouldInsert() {
        when(wrongQuestionMapper.selectByUserAndQuestion(USER_ID, QUESTION_ID)).thenReturn(null);

        wrongQuestionService.recordWrong(USER_ID, QUESTION_ID);

        ArgumentCaptor<WrongQuestion> captor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionMapper).insert((WrongQuestion) captor.capture());
        WrongQuestion inserted = captor.getValue();
        assertThat(inserted.getUserId()).isEqualTo(USER_ID);
        assertThat(inserted.getQuestionId()).isEqualTo(QUESTION_ID);
        assertThat(inserted.getWrongCount()).isEqualTo(1);
        assertThat(inserted.getIsDeleted()).isEqualTo(0);
    }

    @Test
    @DisplayName("recordWrong: 已逻辑删除记录再次做错 → 复活并递增次数")
    void recordWrong_resurrect_shouldUpdateIsDeletedAndIncrement() {
        WrongQuestion deleted = WrongQuestion.builder()
                .id(1L)
                .userId(USER_ID)
                .questionId(QUESTION_ID)
                .wrongCount(2)
                .isDeleted(1)
                .build();
        when(wrongQuestionMapper.selectByUserAndQuestion(USER_ID, QUESTION_ID)).thenReturn(deleted);

        wrongQuestionService.recordWrong(USER_ID, QUESTION_ID);

        ArgumentCaptor<WrongQuestion> captor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionMapper).updateById(captor.capture());
        WrongQuestion updated = captor.getValue();
        assertThat(updated.getIsDeleted()).isEqualTo(0);
        assertThat(updated.getWrongCount()).isEqualTo(3);
        verify(wrongQuestionMapper, never()).insert((WrongQuestion) any());
    }

    @Test
    @DisplayName("recordWrong: 记录已存在且未删除 → 仅递增次数")
    void recordWrong_existing_shouldIncrementCount() {
        WrongQuestion existing = WrongQuestion.builder()
                .id(1L)
                .userId(USER_ID)
                .questionId(QUESTION_ID)
                .wrongCount(5)
                .isDeleted(0)
                .build();
        when(wrongQuestionMapper.selectByUserAndQuestion(USER_ID, QUESTION_ID)).thenReturn(existing);

        wrongQuestionService.recordWrong(USER_ID, QUESTION_ID);

        ArgumentCaptor<WrongQuestion> captor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getWrongCount()).isEqualTo(6);
        assertThat(captor.getValue().getIsDeleted()).isEqualTo(0);
    }

    // -------- removeWrongQuestion --------

    @Test
    @DisplayName("removeWrongQuestion: 记录不存在 → 抛 404")
    void removeWrongQuestion_notFound_shouldThrow404() {
        when(wrongQuestionMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> wrongQuestionService.removeWrongQuestion(USER_ID, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("错题记录不存在");
    }

    @Test
    @DisplayName("removeWrongQuestion: 操作他人记录 → 抛 403")
    void removeWrongQuestion_otherUser_shouldThrow403() {
        WrongQuestion wq = WrongQuestion.builder().id(1L).userId(999L).questionId(QUESTION_ID).isDeleted(0).build();
        when(wrongQuestionMapper.selectById(1L)).thenReturn(wq);

        assertThatThrownBy(() -> wrongQuestionService.removeWrongQuestion(USER_ID, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");
    }

    @Test
    @DisplayName("removeWrongQuestion: 正常移除 → 调用 deleteById")
    void removeWrongQuestion_success_shouldDelete() {
        WrongQuestion wq = WrongQuestion.builder().id(1L).userId(USER_ID).questionId(QUESTION_ID).isDeleted(0).build();
        when(wrongQuestionMapper.selectById(1L)).thenReturn(wq);

        wrongQuestionService.removeWrongQuestion(USER_ID, 1L);

        verify(wrongQuestionMapper).deleteById(1L);
    }

    // -------- listWrongPractice --------

    @Test
    @DisplayName("listWrongPractice: 错题本为空 → 返回空列表")
    void listWrongPractice_empty_shouldReturnEmptyList() {
        when(wrongQuestionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<PracticeQuestionVO> result = wrongQuestionService.listWrongPractice(USER_ID, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listWrongPractice: 按 bankId 过滤后返回对应题目")
    void listWrongPractice_filterByBank_shouldReturnFiltered() {
        WrongQuestion wq1 = WrongQuestion.builder().id(1L).userId(USER_ID).questionId(100L).build();
        WrongQuestion wq2 = WrongQuestion.builder().id(2L).userId(USER_ID).questionId(200L).build();
        when(wrongQuestionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(wq1, wq2));

        Question q1 = Question.builder().id(100L).questionBankId(10L).questionType("SINGLE").stem("题1").optionsJson("[]").sortNo(1).isDeleted(0).build();
        Question q2 = Question.builder().id(200L).questionBankId(20L).questionType("MULTI").stem("题2").optionsJson("[]").sortNo(2).isDeleted(0).build();
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(q1, q2));

        // 只过滤 bankId=10 的
        List<PracticeQuestionVO> result = wrongQuestionService.listWrongPractice(USER_ID, 10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuestionBankId()).isEqualTo(10L);
    }

    // -------- pageWrongQuestions --------

    @Test
    @DisplayName("pageWrongQuestions: 无错题记录 → 返回空分页")
    void pageWrongQuestions_noRecords_shouldReturnEmptyPage() {
        when(wrongQuestionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(inv -> {
                    Page<WrongQuestion> p = inv.getArgument(0);
                    p.setTotal(0);
                    p.setRecords(Collections.emptyList());
                    return p;
                });

        WrongQuestionPageQueryDTO query = new WrongQuestionPageQueryDTO();
        query.setCurrent(1);
        query.setPageSize(10);

        var result = wrongQuestionService.pageWrongQuestions(USER_ID, query);

        assertThat(result.getTotal()).isZero();
        assertThat(result.getRecords()).isEmpty();
    }
}
