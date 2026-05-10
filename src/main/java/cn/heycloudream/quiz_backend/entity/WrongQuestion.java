package cn.heycloudream.quiz_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 错题本表实体，对应数据库表 {@code wrong_question}。
 * <p>
 * 业务约定：首次做错插入；移除时逻辑删除；再次做错同一题时更新复活并递增做错次数。
 * </p>
 *
 * @author atlas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("wrong_question")
public class WrongQuestion {

    /**
     * 主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 试题 ID。
     */
    private Long questionId;

    /**
     * 累计做错次数。
     */
    private Integer wrongCount;

    /**
     * 最近一次做错时间。
     */
    private LocalDateTime lastWrongTime;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-否，1-是。
     */
    @TableLogic
    private Integer isDeleted;
}
