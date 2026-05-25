package cn.heycloudream.ishua_backend.service.guard;

import cn.heycloudream.ishua_backend.entity.QuestionBank;
import cn.heycloudream.ishua_backend.enums.UserRole;
import cn.heycloudream.ishua_backend.exception.BusinessException;
import cn.heycloudream.ishua_backend.mapper.QuestionBankMapper;
import cn.heycloudream.ishua_backend.util.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 题库资源访问守卫，统一处理题库归属校验与管理员运维放行。
 *
 * @author C1ouD
 */
@Component
@RequiredArgsConstructor
public class BankAccessGuard {

    private final QuestionBankMapper questionBankMapper;

    /**
     * 要求当前用户拥有指定题库；管理员可直接放行。
     */
    public QuestionBank requireOwnedBank(Long currentUserId, Long bankId) {
        return requireOwnedBank(currentUserId, UserContextHolder.getRole(), bankId);
    }

    /**
     * 要求当前用户拥有指定题库；管理员可直接放行。
     */
    public QuestionBank requireOwnedBank(Long currentUserId, UserRole role, Long bankId) {
        QuestionBank bank = questionBankMapper.selectById(bankId);
        if (bank == null) {
            throw new BusinessException(404, "题库不存在或无权访问");
        }
        if (UserRole.ADMIN.equals(role)) {
            return bank;
        }
        if (currentUserId == null || bank.getUserId() == null || !bank.getUserId().equals(currentUserId)) {
            throw new BusinessException(404, "题库不存在或无权访问");
        }
        return bank;
    }

    /**
     * 私有题库刷题入口：普通用户不可访问私有库，高级用户须拥有该题库，管理员可放行。
     */
    public void requirePrivatePracticeAccess(Long currentUserId, UserRole role, QuestionBank bank) {
        if (bank == null) {
            throw new BusinessException(404, "题库不存在");
        }
        if (UserRole.ADMIN.equals(role)) {
            return;
        }
        if (role == null || !role.includes(UserRole.PREMIUM)) {
            throw new BusinessException(403, "无权访问该题库");
        }
        if (currentUserId == null || bank.getUserId() == null || !bank.getUserId().equals(currentUserId)) {
            throw new BusinessException(403, "无权访问该题库");
        }
    }
}
