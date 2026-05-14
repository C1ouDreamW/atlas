package cn.heycloudream.quiz_backend.mapper;

import cn.heycloudream.quiz_backend.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 试题表 Mapper。
 *
 * @author C1ouD
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
