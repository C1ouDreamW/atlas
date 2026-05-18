package cn.heycloudream.quiz_backend.service;

import cn.heycloudream.quiz_backend.dto.practice.AnswerSubmitDTO;
import cn.heycloudream.quiz_backend.vo.practice.AnswerSubmitResultVO;
import cn.heycloudream.quiz_backend.vo.practice.PracticeQuestionVO;

import java.util.List;

/**
 * 刷题业务服务接口。
 *
 * @author C1ouD
 */
public interface PracticeService {

    /**
     * 获取指定题库的刷题题目列表（不含答案与解析）。
     * <p>
     * 公开题库复用热点 Redis 缓存；私有题库先做归属校验后直接查 DB。
     * </p>
     *
     * @param userId 当前用户 ID（私有题库归属校验用，公开题库可为 null）
     * @param bankId 题库 ID
     * @param random 是否随机打乱顺序
     * @return 刷题 VO 列表
     */
    List<PracticeQuestionVO> listPracticeQuestions(Long userId, Long bankId, boolean random);

    /**
     * 提交单题答案并返回判分结果。
     * <p>
     * 客观题（SINGLE / MULTI / JUDGE）自动判分；主观题返回"需人工判分"标记。
     * 客观题答错时自动将该题加入用户错题本。
     * </p>
     *
     * @param userId     当前用户 ID
     * @param bankId     题库 ID（用于归属校验，公开题库跳过）
     * @param questionId 试题 ID
     * @param dto        用户提交的答案
     * @return 判分结果
     */
    AnswerSubmitResultVO submitAnswer(Long userId, Long bankId, Long questionId, AnswerSubmitDTO dto);
}
