package cn.heycloudream.ishua_backend.service;

import cn.heycloudream.ishua_backend.dto.user.UserLoginDTO;
import cn.heycloudream.ishua_backend.dto.user.UserRegisterDTO;
import cn.heycloudream.ishua_backend.entity.SysUser;
import cn.heycloudream.ishua_backend.exception.BusinessException;
import cn.heycloudream.ishua_backend.mapper.SysUserMapper;
import cn.heycloudream.ishua_backend.service.impl.UserServiceImpl;
import cn.heycloudream.ishua_backend.util.JwtUtils;
import cn.heycloudream.ishua_backend.vo.user.UserLoginVO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link cn.heycloudream.ishua_backend.service.impl.UserServiceImpl} 注册/登录单元测试。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("register: 用户名已存在 → 409")
    void register_duplicateUsername_shouldThrow409() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("dup");
        dto.setPassword("123456");

        when(sysUserMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(409);
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    @DisplayName("register: 成功 → BCrypt 加密后入库")
    void register_success_shouldEncodePassword() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("newbie");
        dto.setPassword("secret12");
        dto.setNickname("新手");

        when(sysUserMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("secret12")).thenReturn("$2a$encoded");

        userService.register(dto);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$encoded");
        assertThat(captor.getValue().getUsername()).isEqualTo("newbie");
    }

    @Test
    @DisplayName("login: 用户不存在 → 401")
    void login_userNotFound_shouldThrow401() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("ghost");
        dto.setPassword("123456");

        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> userService.login(dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("login: 密码错误 → 401")
    void login_wrongPassword_shouldThrow401() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("alice");
        dto.setPassword("wrong");

        SysUser user = SysUser.builder()
                .id(2L)
                .username("alice")
                .passwordHash("$2a$hash")
                .role("USER")
                .build();
        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("wrong", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("login: 成功 → 返回 JWT 与用户信息")
    void login_success_shouldReturnToken() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("alice");
        dto.setPassword("correct");

        SysUser user = SysUser.builder()
                .id(2L)
                .username("alice")
                .nickname("艾丽丝")
                .passwordHash("$2a$hash")
                .role("PREMIUM")
                .build();
        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("correct", "$2a$hash")).thenReturn(true);
        when(jwtUtils.generateToken(2L)).thenReturn("jwt-token");

        UserLoginVO vo = userService.login(dto);

        assertThat(vo.getToken()).isEqualTo("jwt-token");
        assertThat(vo.getUserId()).isEqualTo(2L);
        assertThat(vo.getRole()).isEqualTo("PREMIUM");
    }
}
