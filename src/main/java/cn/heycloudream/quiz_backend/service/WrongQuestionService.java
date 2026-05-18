package cn.heycloudream.quiz_backend.service;

import cn.heycloudream.quiz_backend.common.vo.PageResultVO;
import cn.heycloudream.quiz_backend.dto.wrong.WrongQuestionPageQueryDTO;
import cn.heycloudream.quiz_backend.vo.practice.PracticeQuestionVO;
import cn.heycloudream.quiz_backend.vo.wrong.WrongQuestionVO;

import java.util.List;

/**
 * 错题本业务服务接口。
 *
 * @author C1ouD
 */
public interface WrongQuestionService {

    /**
     * 记录一次做错：首次 INSERT，已存在（含逻辑删除）则 UPDATE 复活并递增次数。
     *
     * @param userId     当前用户 ID
     * @param questionId 做错的试题 ID
     */
    void recordWrong(Long userId, Long questionId);

    /**
     * 分页查询当前用户的错题本，可按题库过滤。
     *
     * @param userId 当前用户 ID
     * @param query  分页与过滤参数
     * @return 分页错题列表
     */
    PageResultVO<WrongQuestionVO> pageWrongQuestions(Long userId, WrongQuestionPageQueryDTO query);

    /**
     * 从错题本中移除一条记录（逻辑删除），同时校验归属防止越权。
     *
     * @param userId          当前用户 ID
     * @param wrongQuestionId 错题本记录 ID
     */
    void removeWrongQuestion(Long userId, Long wrongQuestionId);

    /**
     * 获取当前用户错题本的刷题列表（不含答案），可按题库过滤，用于"按错题本重刷"场景。
     *
     * @param userId 当前用户 ID
     * @param bankId 题库 ID，null 表示全部错题
     * @return 隐藏答案的刷题 VO 列表，按最近做错时间倒序
     */
    List<PracticeQuestionVO> listWrongPractice(Long userId, Long bankId);
}
