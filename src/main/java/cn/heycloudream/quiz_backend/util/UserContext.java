package cn.heycloudream.quiz_backend.util;

import cn.heycloudream.quiz_backend.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求级用户上下文。
 *
 * @author C1ouD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {

    /**
     * 当前登录用户 ID。
     */
    private Long userId;

    /**
     * 当前登录用户角色。
     */
    private UserRole role;
}
