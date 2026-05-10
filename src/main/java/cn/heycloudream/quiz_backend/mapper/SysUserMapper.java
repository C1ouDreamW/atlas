package cn.heycloudream.quiz_backend.mapper;

import cn.heycloudream.quiz_backend.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper。
 *
 * @author atlas
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
