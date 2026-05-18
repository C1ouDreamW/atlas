package cn.heycloudream.quiz_backend.service.impl;

import cn.heycloudream.quiz_backend.common.vo.PageResultVO;
import cn.heycloudream.quiz_backend.dto.wrong.WrongQuestionPageQueryDTO;
import cn.heycloudream.quiz_backend.entity.Question;
import cn.heycloudream.quiz_backend.entity.WrongQuestion;
import cn.heycloudream.quiz_backend.exception.BusinessException;
import cn.heycloudream.quiz_backend.mapper.QuestionMapper;
import cn.heycloudream.quiz_backend.mapper.WrongQuestionMapper;
import cn.heycloudream.quiz_backend.service.WrongQuestionService;
import cn.heycloudream.quiz_backend.vo.practice.PracticeQuestionVO;
import cn.heycloudream.quiz_backend.vo.wrong.WrongQuestionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 错题本业务服务实现。
 * <p>
 * 业务约定：首次做错 INSERT；移除时 UPDATE is_deleted=1；再次做错同一题 UPDATE 复活并递增次数。禁止重复 INSERT。
 * </p>
 *
 * @author C1ouD
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WrongQuestionServiceImpl implements WrongQuestionService {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionMapper questionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordWrong(Long userId, Long questionId) {
        WrongQuestion existing = wrongQuestionMapper.selectByUserAndQuestion(userId, questionId);
        LocalDateTime now = LocalDateTime.now();

        if (existing == null) {
            // 首次做错：INSERT
            WrongQuestion record = WrongQuestion.builder()
                    .userId(userId)
                    .questionId(questionId)
                    .wrongCount(1)
                    .lastWrongTime(now)
                    .createTime(now)
                    .updateTime(now)
                    .isDeleted(0)
                    .build();
            wrongQuestionMapper.insert(record);
            log.debug("[错题本] 首次错题入库 userId={} questionId={}", userId, questionId);
        } else if (existing.getIsDeleted() == 1) {
            // 曾移除后重新做错：复活 + 递增次数
            existing.setIsDeleted(0);
            existing.setWrongCount(existing.getWrongCount() + 1);
            existing.setLastWrongTime(now);
            existing.setUpdateTime(now);
            wrongQuestionMapper.updateById(existing);
            log.debug("[错题本] 错题复活 userId={} questionId={} wrongCount={}", userId, questionId, existing.getWrongCount());
        } else {
            // 已在错题本中：仅递增次数
            existing.setWrongCount(existing.getWrongCount() + 1);
            existing.setLastWrongTime(now);
            existing.setUpdateTime(now);
            wrongQuestionMapper.updateById(existing);
            log.debug("[错题本] 错题累计 userId={} questionId={} wrongCount={}", userId, questionId, existing.getWrongCount());
        }
    }

    @Override
    public PageResultVO<WrongQuestionVO> pageWrongQuestions(Long userId, WrongQuestionPageQueryDTO query) {
        // 分页查 wrong_question（MyBatis-Plus 自动过滤 is_deleted=0）
        Page<WrongQuestion> mp = new Page<>(query.getCurrent(), query.getPageSize());
        LambdaQueryWrapper<WrongQuestion> w = new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .orderByDesc(WrongQuestion::getLastWrongTime);
        wrongQuestionMapper.selectPage(mp, w);

        List<WrongQuestion> records = mp.getRecords();
        if (records.isEmpty()) {
            return PageResultVO.<WrongQuestionVO>builder().total(0L).records(Collections.emptyList()).build();
        }

        // 批量查关联试题，减少 N+1
        Set<Long> questionIds = records.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 按题库过滤（在 Java 层过滤避免复杂 JOIN，数量有限）
        List<WrongQuestionVO> vos = records.stream()
                .filter(wq -> {
                    Question q = questionMap.get(wq.getQuestionId());
                    if (q == null) {
                        return false;
                    }
                    return query.getBankId() == null || query.getBankId().equals(q.getQuestionBankId());
                })
                .map(wq -> toVO(wq, questionMap.get(wq.getQuestionId())))
                .collect(Collectors.toList());

        // bankId 过滤后 total 需相应调整（近似值：基于过滤后列表大小）
        long total = query.getBankId() == null ? mp.getTotal() : vos.size();
        return PageResultVO.<WrongQuestionVO>builder().total(total).records(vos).build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeWrongQuestion(Long userId, Long wrongQuestionId) {
        WrongQuestion wq = wrongQuestionMapper.selectById(wrongQuestionId);
        if (wq == null) {
            throw new BusinessException(404, "错题记录不存在");
        }
        if (!userId.equals(wq.getUserId())) {
            throw new BusinessException(403, "无权操作他人错题记录");
        }
        // 逻辑删除
        wrongQuestionMapper.deleteById(wrongQuestionId);
        log.debug("[错题本] 移除记录 userId={} wrongQuestionId={}", userId, wrongQuestionId);
    }

    @Override
    public List<PracticeQuestionVO> listWrongPractice(Long userId, Long bankId) {
        // 查当前用户的错题本（is_deleted=0）
        LambdaQueryWrapper<WrongQuestion> w = new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .orderByDesc(WrongQuestion::getLastWrongTime);
        List<WrongQuestion> wrongList = wrongQuestionMapper.selectList(w);
        if (wrongList.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查试题
        Set<Long> questionIds = wrongList.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        return wrongList.stream()
                .map(wq -> questionMap.get(wq.getQuestionId()))
                .filter(q -> q != null && (bankId == null || bankId.equals(q.getQuestionBankId())))
                .map(this::toPracticeVO)
                .collect(Collectors.toList());
    }

    private WrongQuestionVO toVO(WrongQuestion wq, Question q) {
        return WrongQuestionVO.builder()
                .id(wq.getId())
                .questionId(wq.getQuestionId())
                .questionBankId(q.getQuestionBankId())
                .questionType(q.getQuestionType())
                .stem(q.getStem())
                .optionsJson(q.getOptionsJson())
                .wrongCount(wq.getWrongCount())
                .lastWrongTime(wq.getLastWrongTime())
                .build();
    }

    private PracticeQuestionVO toPracticeVO(Question q) {
        return PracticeQuestionVO.builder()
                .id(q.getId())
                .questionBankId(q.getQuestionBankId())
                .questionType(q.getQuestionType())
                .stem(q.getStem())
                .optionsJson(q.getOptionsJson())
                .sortNo(q.getSortNo())
                .build();
    }
}
