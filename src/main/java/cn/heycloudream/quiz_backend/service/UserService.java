package cn.heycloudream.quiz_backend.service;

import cn.heycloudream.quiz_backend.common.vo.PageResultVO;
import cn.heycloudream.quiz_backend.dto.admin.AdminUserPageQueryDTO;
import cn.heycloudream.quiz_backend.dto.admin.AdminUserRoleUpdateDTO;
import cn.heycloudream.quiz_backend.dto.user.UserLoginDTO;
import cn.heycloudream.quiz_backend.dto.user.UserRegisterDTO;
import cn.heycloudream.quiz_backend.vo.admin.AdminUserVO;
import cn.heycloudream.quiz_backend.vo.user.UserLoginVO;
import cn.heycloudream.quiz_backend.vo.user.UserMeVO;

/**
 * 用户鉴权领域服务。
 *
 * @author C1ouD
 */
public interface UserService {

    /**
     * 注册新用户：校验用户名唯一性 → BCrypt 加密密码 → 落库。
     */
    void register(UserRegisterDTO dto);

    /**
     * 登录校验：比对密码 → 签发 JWT Token。
     *
     * @return 包含 Token 与用户基本信息的 VO
     */
    UserLoginVO login(UserLoginDTO dto);

    /**
     * 获取当前登录用户信息。
     */
    UserMeVO getCurrentUser(Long currentUserId);

    /**
     * 管理端分页查询用户。
     */
    PageResultVO<AdminUserVO> pageAdminUsers(AdminUserPageQueryDTO query);

    /**
     * 管理端更新用户角色。
     */
    void updateUserRole(Long operatorUserId, Long targetUserId, AdminUserRoleUpdateDTO dto);
}
