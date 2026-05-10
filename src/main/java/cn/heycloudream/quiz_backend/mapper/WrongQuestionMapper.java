package cn.heycloudream.quiz_backend.mapper;

import cn.heycloudream.quiz_backend.entity.WrongQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 错题本表 Mapper。
 *
 * @author atlas
 */
@Mapper
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {
}
