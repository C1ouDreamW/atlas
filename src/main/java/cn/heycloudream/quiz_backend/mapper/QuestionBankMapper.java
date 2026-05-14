package cn.heycloudream.quiz_backend.mapper;

import cn.heycloudream.quiz_backend.entity.QuestionBank;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题库表 Mapper。
 *
 * @author C1ouD
 */
@Mapper
public interface QuestionBankMapper extends BaseMapper<QuestionBank> {
}
