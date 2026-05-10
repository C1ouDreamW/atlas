package cn.heycloudream.quiz_backend.enums;

import java.util.Arrays;

/**
 * 试题题型枚举，与大模型约定及数据库 {@code question_type} 一致。
 *
 * @author atlas
 */
public enum QuestionType {

    SINGLE,
    MULTI,
    JUDGE;

    public static boolean isValidCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(v -> v.name().equals(code.trim()));
    }
}
