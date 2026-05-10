package cn.heycloudream.quiz_backend.service;

import cn.heycloudream.quiz_backend.common.dto.PageRequestDTO;
import cn.heycloudream.quiz_backend.common.vo.PageResultVO;
import cn.heycloudream.quiz_backend.dto.questionbank.QuestionBankCreateDTO;
import cn.heycloudream.quiz_backend.dto.questionbank.QuestionBankUpdateDTO;
import cn.heycloudream.quiz_backend.vo.questionbank.QuestionBankVO;

/**
 * 题库领域服务。
 *
 * @author atlas
 */
public interface QuestionBankService {

    /**
     * 分页查询当前用户的题库列表。
     */
    PageResultVO<QuestionBankVO> pageMyBanks(Long currentUserId, PageRequestDTO page);

    /**
     * 创建题库。
     *
     * @return 新建题库主键
     */
    Long createBank(Long currentUserId, QuestionBankCreateDTO dto);

    /**
     * 全量更新题库。
     */
    void updateBank(Long currentUserId, Long bankId, QuestionBankUpdateDTO dto);

    /**
     * 删除题库（同时逻辑删除其下全部试题）。
     */
    void deleteBank(Long currentUserId, Long bankId);
}
