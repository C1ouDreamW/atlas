package cn.heycloudream.quiz_backend.service.impl;

import cn.heycloudream.quiz_backend.dto.user.UserLoginDTO;
import cn.heycloudream.quiz_backend.dto.user.UserRegisterDTO;
import cn.heycloudream.quiz_backend.entity.SysUser;
import cn.heycloudream.quiz_backend.exception.BusinessException;
import cn.heycloudream.quiz_backend.mapper.SysUserMapper;
import cn.heycloudream.quiz_backend.service.UserService;
import cn.heycloudream.quiz_backend.util.JwtUtils;
import cn.heycloudream.quiz_backend.vo.user.UserLoginVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户鉴权服务实现。
 *
 * @author C1ouD
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO dto) {
        // 校验用户名是否已存在（MyBatis-Plus @TableLogic 自动过滤 is_deleted=1）
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
        );
        if (count > 0) {
            log.warn("注册失败：用户名已存在 username={}", dto.getUsername());
            throw new BusinessException(409, "用户名已存在");
        }

        SysUser user = SysUser.builder()
                .username(dto.getUsername())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .nickname(dto.getNickname())
                .role("STUDENT")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        sysUserMapper.insert(user);
        log.info("用户注册成功 userId={} username={}", user.getId(), user.getUsername());
    }

    @Override
    public UserLoginVO login(UserLoginDTO dto) {
        // 按用户名查询（@TableLogic 自动过滤已删除账号）
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
        );

        if (user == null) {
            log.warn("登录失败：用户名不存在 username={}", dto.getUsername());
            throw new BusinessException(401, "账号或密码错误");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            log.warn("登录失败：密码错误 username={}", dto.getUsername());
            throw new BusinessException(401, "账号或密码错误");
        }

        String token = jwtUtils.generateToken(user.getId());
        log.info("用户登录成功 userId={} username={}", user.getId(), user.getUsername());

        return UserLoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .build();
    }
}
