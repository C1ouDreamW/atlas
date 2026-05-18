package cn.heycloudream.quiz_backend.mapper;

import cn.heycloudream.quiz_backend.entity.WrongQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 错题本表 Mapper。
 *
 * @author C1ouD
 */
@Mapper
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {

    /**
     * 按用户和试题查找错题记录，包含已逻辑删除的记录（绕过 {@code @TableLogic}）。
     * <p>
     * 用于实现"复活"逻辑：若该记录已被逻辑删除，重新做错时需 UPDATE 而非 INSERT。
     * </p>
     *
     * @param userId     用户 ID
     * @param questionId 试题 ID
     * @return 错题记录（包含 is_deleted=1 的）；不存在返回 null
     */
    @Select("SELECT * FROM wrong_question WHERE user_id = #{userId} AND question_id = #{questionId} LIMIT 1")
    WrongQuestion selectByUserAndQuestion(@Param("userId") Long userId, @Param("questionId") Long questionId);
}
