package cn.heycloudream.quiz_backend.service.impl;

import cn.heycloudream.quiz_backend.dto.practice.AnswerSubmitDTO;
import cn.heycloudream.quiz_backend.entity.Question;
import cn.heycloudream.quiz_backend.entity.QuestionBank;
import cn.heycloudream.quiz_backend.exception.BusinessException;
import cn.heycloudream.quiz_backend.mapper.QuestionBankMapper;
import cn.heycloudream.quiz_backend.service.PracticeService;
import cn.heycloudream.quiz_backend.service.QuestionBankHotDetailService;
import cn.heycloudream.quiz_backend.service.QuestionService;
import cn.heycloudream.quiz_backend.service.WrongQuestionService;
import cn.heycloudream.quiz_backend.service.guard.BankAccessGuard;
import cn.heycloudream.quiz_backend.util.UserContextHolder;
import cn.heycloudream.quiz_backend.vo.practice.AnswerSubmitResultVO;
import cn.heycloudream.quiz_backend.vo.practice.PracticeQuestionVO;
import cn.heycloudream.quiz_backend.vo.question.QuestionVO;
import cn.heycloudream.quiz_backend.vo.questionbank.QuestionBankDetailBundleVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 刷题业务服务实现。
 * <p>
 * 拉题策略：公开题库复用 {@link QuestionBankHotDetailService}（带 Redis 缓存），私有题库归属校验后直接查 DB。
 * 判分策略：SINGLE/JUDGE 比较首个答案（忽略大小写）；MULTI 排序后全量比较；其他题型标记需人工判分。
 * </p>
 *
 * @author C1ouD
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeServiceImpl implements PracticeService {

    private final QuestionBankHotDetailService questionBankHotDetailService;
    private final QuestionBankMapper questionBankMapper;
    private final QuestionService questionService;
    private final WrongQuestionService wrongQuestionService;
    private final BankAccessGuard bankAccessGuard;
    private final ObjectMapper objectMapper;

    @Override
    public List<PracticeQuestionVO> listPracticeQuestions(Long userId, Long bankId, boolean random) {
        QuestionBank bank = requireExistsBank(bankId);

        List<PracticeQuestionVO> questions;
        if (isPublic(bank)) {
            // 公开题库：复用热点缓存，再剥离答案
            QuestionBankDetailBundleVO bundle = questionBankHotDetailService.getHotPublicBankDetail(bankId);
            questions = bundle.getQuestions().stream()
                    .map(this::toPracticeVO)
                    .collect(Collectors.toList());
        } else {
            // 私有题库：归属校验 + 查 DB（通过 QuestionService.listByBankId）
            bankAccessGuard.requirePrivatePracticeAccess(userId, UserContextHolder.getRole(), bank);
            questions = questionService.listByBankId(bankId).stream()
                    .map(this::entityToPracticeVO)
                    .collect(Collectors.toList());
        }

        if (random && questions.size() > 1) {
            List<PracticeQuestionVO> shuffled = new ArrayList<>(questions);
            Collections.shuffle(shuffled);
            return shuffled;
        }
        return questions;
    }

    @Override
    public AnswerSubmitResultVO submitAnswer(Long userId, Long bankId, Long questionId, AnswerSubmitDTO dto) {
        // 查试题
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(404, "试题不存在");
        }
        if (!bankId.equals(question.getQuestionBankId())) {
            throw new BusinessException(400, "试题不属于该题库");
        }

        // 校验访问权限：公开题库无需归属校验；私有题库需要
        QuestionBank bank = requireExistsBank(bankId);
        if (!isPublic(bank)) {
            bankAccessGuard.requirePrivatePracticeAccess(userId, UserContextHolder.getRole(), bank);
        }

        // 判分
        String questionType = question.getQuestionType();
        boolean isObjective = isObjectiveType(questionType);

        if (!isObjective) {
            // 主观题或未知题型，标记需人工判分
            return AnswerSubmitResultVO.builder()
                    .questionId(questionId)
                    .correct(null)
                    .needsManualGrading(true)
                    .answerJson(question.getAnswerJson())
                    .analysis(question.getAnalysis())
                    .build();
        }

        boolean correct = grade(questionType, dto.getUserAnswer(), question.getAnswerJson());
        if (!correct) {
            try {
                wrongQuestionService.recordWrong(userId, questionId);
            } catch (Exception e) {
                // 错题记录写入失败不影响判分结果返回，仅日志告警
                log.warn("[刷题] 写入错题本失败 userId={} questionId={}", userId, questionId, e);
            }
        }

        return AnswerSubmitResultVO.builder()
                .questionId(questionId)
                .correct(correct)
                .needsManualGrading(false)
                .answerJson(question.getAnswerJson())
                .analysis(question.getAnalysis())
                .build();
    }

    /**
     * 客观题判分：SINGLE/JUDGE 比较首个答案；MULTI 排序后全量比较。均忽略大小写。
     */
    private boolean grade(String questionType, List<String> userAnswer, String answerJson) {
        List<String> correctAnswer;
        try {
            correctAnswer = objectMapper.readValue(answerJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("[刷题] 解析标准答案 JSON 失败 answerJson={}", answerJson, e);
            throw new BusinessException(500, "试题答案数据异常，无法判分");
        }

        if (userAnswer == null || userAnswer.isEmpty()) {
            return false;
        }

        if ("MULTI".equalsIgnoreCase(questionType)) {
            // 多选题：数量必须一致，排序后逐一比较
            if (userAnswer.size() != correctAnswer.size()) {
                return false;
            }
            List<String> sortedUser = userAnswer.stream()
                    .map(String::toUpperCase)
                    .sorted()
                    .collect(Collectors.toList());
            List<String> sortedCorrect = correctAnswer.stream()
                    .map(String::toUpperCase)
                    .sorted()
                    .collect(Collectors.toList());
            return sortedUser.equals(sortedCorrect);
        }

        // SINGLE / JUDGE：只比较第一个答案
        if (correctAnswer.isEmpty()) {
            return false;
        }
        return userAnswer.get(0).equalsIgnoreCase(correctAnswer.get(0));
    }

    /**
     * 判断是否为可自动判分的客观题类型。
     */
    private boolean isObjectiveType(String questionType) {
        if (questionType == null) {
            return false;
        }
        String upper = questionType.toUpperCase();
        return "SINGLE".equals(upper) || "MULTI".equals(upper) || "JUDGE".equals(upper);
    }

    private QuestionBank requireExistsBank(Long bankId) {
        QuestionBank bank = questionBankMapper.selectById(bankId);
        if (bank == null) {
            throw new BusinessException(404, "题库不存在");
        }
        return bank;
    }

    private boolean isPublic(QuestionBank bank) {
        return bank.getIsPublic() != null && bank.getIsPublic() == 1;
    }

    private PracticeQuestionVO toPracticeVO(QuestionVO vo) {
        return PracticeQuestionVO.builder()
                .id(vo.getId())
                .questionBankId(vo.getQuestionBankId())
                .questionType(vo.getQuestionType())
                .stem(vo.getStem())
                .optionsJson(vo.getOptionsJson())
                .sortNo(vo.getSortNo())
                .build();
    }

    private PracticeQuestionVO entityToPracticeVO(Question q) {
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
