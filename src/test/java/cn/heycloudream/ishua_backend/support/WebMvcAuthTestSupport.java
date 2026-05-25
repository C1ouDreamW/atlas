package cn.heycloudream.ishua_backend.support;

import cn.heycloudream.ishua_backend.entity.SysUser;
import cn.heycloudream.ishua_backend.enums.UserRole;
import cn.heycloudream.ishua_backend.mapper.SysUserMapper;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * {@code @WebMvcTest} 鉴权场景：为 JWT 拦截器 stub {@link SysUserMapper}。
 */
public final class WebMvcAuthTestSupport {

    private WebMvcAuthTestSupport() {
    }

    public static void stubUser(SysUserMapper sysUserMapper, Long userId, UserRole role) {
        when(sysUserMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            if (userId.equals(id)) {
                return SysUser.builder()
                        .id(userId)
                        .username("testuser")
                        .passwordHash("hash")
                        .role(role.name())
                        .build();
            }
            return null;
        });
    }

    public static void stubPremiumUser(SysUserMapper sysUserMapper, Long userId) {
        stubUser(sysUserMapper, userId, UserRole.PREMIUM);
    }

    public static void stubBasicUser(SysUserMapper sysUserMapper, Long userId) {
        stubUser(sysUserMapper, userId, UserRole.USER);
    }
}
