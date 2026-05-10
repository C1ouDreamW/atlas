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
 * 用户表实体，对应数据库表 {@code sys_user}。
 *
 * @author atlas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class SysUser {

    /**
     * 主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 登录账号（业务唯一，与逻辑删除字段组合唯一索引）。
     */
    private String username;

    /**
     * BCrypt 密码密文。
     */
    private String passwordHash;

    /**
     * 昵称。
     */
    private String nickname;

    /**
     * 角色权限（如 STUDENT；可扩展管理员/教师端）。
     */
    private String role;

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
