package cn.heycloudream.ishua_backend.enums;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;

/**
 * 试题题型枚举，与大模型约定及数据库 {@code question_type} 一致。
 *
 * @author C1ouD
 */
@Schema(description = "试题题型（接口字段为同名 string，取值见枚举）")
public enum QuestionType {

    @Schema(description = "单选题；答案为选项字母，如 [\"A\"]")
    SINGLE,

    @Schema(description = "多选题；答案为多个选项字母，如 [\"A\",\"C\"]")
    MULTI,

    @Schema(description = "判断题；答案为 [\"T\"]（对）或 [\"F\"]（错）；选项固定为 [\"正确\",\"错误\"]")
    JUDGE,

    @Schema(description = "简答题；options 为 []，answer 为文本要点数组；不自动判分，刷题时通过 reference 接口查看参考答案")
    SHORT_ANSWER;

    public static boolean isValidCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(v -> v.name().equals(code.trim()));
    }
}
