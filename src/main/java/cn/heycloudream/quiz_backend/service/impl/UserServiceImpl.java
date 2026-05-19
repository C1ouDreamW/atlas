package cn.heycloudream.quiz_backend.service.impl;

import cn.heycloudream.quiz_backend.common.vo.PageResultVO;
import cn.heycloudream.quiz_backend.dto.admin.AdminUserPageQueryDTO;
import cn.heycloudream.quiz_backend.dto.admin.AdminUserRoleUpdateDTO;
import cn.heycloudream.quiz_backend.dto.user.UserLoginDTO;
import cn.heycloudream.quiz_backend.dto.user.UserRegisterDTO;
import cn.heycloudream.quiz_backend.entity.SysUser;
import cn.heycloudream.quiz_backend.enums.UserRole;
import cn.heycloudream.quiz_backend.exception.BusinessException;
import cn.heycloudream.quiz_backend.mapper.SysUserMapper;
import cn.heycloudream.quiz_backend.service.UserService;
import cn.heycloudream.quiz_backend.util.JwtUtils;
import cn.heycloudream.quiz_backend.vo.admin.AdminUserVO;
import cn.heycloudream.quiz_backend.vo.user.UserLoginVO;
import cn.heycloudream.quiz_backend.vo.user.UserMeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
                .role(UserRole.USER.name())
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
        UserRole role = UserRole.fromDbValue(user.getRole());
        log.info("用户登录成功 userId={} username={}", user.getId(), user.getUsername());

        return UserLoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(role.name())
                .build();
    }

    @Override
    public UserMeVO getCurrentUser(Long currentUserId) {
        SysUser user = requireUser(currentUserId);
        UserRole role = UserRole.fromDbValue(user.getRole());
        return UserMeVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(role.name())
                .build();
    }

    @Override
    public PageResultVO<AdminUserVO> pageAdminUsers(AdminUserPageQueryDTO query) {
        Page<SysUser> page = new Page<>(query.getCurrent(), query.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreateTime);
        if (query.getUsername() != null && !query.getUsername().isBlank()) {
            wrapper.like(SysUser::getUsername, query.getUsername().trim());
        }
        if (query.getRole() != null && !query.getRole().isBlank()) {
            wrapper.eq(SysUser::getRole, UserRole.fromRequestValue(query.getRole()).name());
        }
        sysUserMapper.selectPage(page, wrapper);
        List<AdminUserVO> records = page.getRecords().stream()
                .map(this::toAdminUserVO)
                .collect(Collectors.toList());
        return PageResultVO.<AdminUserVO>builder()
                .total(page.getTotal())
                .records(records)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserRole(Long operatorUserId, Long targetUserId, AdminUserRoleUpdateDTO dto) {
        SysUser target = requireUser(targetUserId);
        UserRole oldRole = UserRole.fromDbValue(target.getRole());
        if (UserRole.ADMIN.equals(oldRole)) {
            throw new BusinessException(403, "禁止修改管理员角色");
        }
        UserRole newRole = UserRole.fromRequestValue(dto.getRole());
        if (UserRole.ADMIN.equals(newRole)) {
            throw new BusinessException(403, "禁止通过管理端接口设置管理员角色");
        }

        target.setRole(newRole.name());
        target.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(target);
        log.info("管理员变更用户角色 operatorUserId={} targetUserId={} oldRole={} newRole={}",
                operatorUserId, targetUserId, oldRole, newRole);
    }

    private SysUser requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "未登录或用户上下文缺失");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private AdminUserVO toAdminUserVO(SysUser user) {
        UserRole role = UserRole.fromDbValue(user.getRole());
        return AdminUserVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(role.name())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }
}
