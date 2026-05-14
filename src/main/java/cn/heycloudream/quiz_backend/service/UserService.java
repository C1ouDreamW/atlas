package cn.heycloudream.quiz_backend.service;

import cn.heycloudream.quiz_backend.dto.user.UserLoginDTO;
import cn.heycloudream.quiz_backend.dto.user.UserRegisterDTO;
import cn.heycloudream.quiz_backend.vo.user.UserLoginVO;

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
}
