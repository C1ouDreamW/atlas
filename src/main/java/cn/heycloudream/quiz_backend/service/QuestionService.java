package cn.heycloudream.quiz_backend.service;

import cn.heycloudream.quiz_backend.common.vo.PageResultVO;
import cn.heycloudream.quiz_backend.dto.question.QuestionInBankPageQueryDTO;
import cn.heycloudream.quiz_backend.dto.question.QuestionUpdateDTO;
import cn.heycloudream.quiz_backend.entity.Question;
import cn.heycloudream.quiz_backend.vo.ai.QuestionPreviewVO;
import cn.heycloudream.quiz_backend.vo.question.QuestionVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 试题领域服务，封装批量写入等能力。
 *
 * @author C1ouD
 */
public interface QuestionService extends IService<Question> {

    /**
     * 将 AI 导入解析后的试题在同一事务中批量落库。
     *
     * @param questions 已通过校验的实体列表（可为空，空则直接返回）
     */
    void saveImportedQuestions(List<Question> questions);

    /**
     * 将预览确认后的题目列表批量落库（新任务体系）。
     * <p>
     * 将 {@link QuestionPreviewVO} 转换为 {@link Question} 实体后委托 {@link #saveImportedQuestions}。
     * </p>
     *
     * @param bankId   目标题库 ID
     * @param previews 前端确认后的预览题目列表
     */
    void batchImportPreview(Long currentUserId, Long bankId, List<QuestionPreviewVO> previews);

    /**
     * 分页查询指定题库下的试题（需校验题库归属当前用户）。
     */
    PageResultVO<QuestionVO> pageQuestionsInBank(Long currentUserId, Long bankId, QuestionInBankPageQueryDTO query);

    /**
     * 根据主键查询试题详情（需校验所属题库归属当前用户）。
     */
    QuestionVO getQuestionById(Long currentUserId, Long questionId);

    /**
     * 在指定题库下新增试题；请求体字段与 {@link QuestionUpdateDTO} 一致，不包含题库 ID。
     *
     * @return 新建试题主键
     */
    Long createQuestionInBank(Long currentUserId, Long bankId, QuestionUpdateDTO body);

    /**
     * 全量更新试题（不可更换所属题库）。
     */
    void updateQuestion(Long currentUserId, Long questionId, QuestionUpdateDTO dto);

    /**
     * 删除试题。
     */
    void deleteQuestion(Long currentUserId, Long questionId);

    /**
     * 逻辑删除某题库下的全部试题（供删除题库时级联调用）。
     */
    void removeQuestionsByBankId(Long bankId);

    /**
     * 查询指定题库下的全部试题，按 sortNo 升序排列（刷题模式专用，不做归属校验）。
     *
     * @param bankId 题库 ID
     * @return 试题列表
     */
    List<Question> listByBankId(Long bankId);
}
