package cn.heycloudream.ishua_backend.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 全局校验用长度与范围常量，避免魔法数字散落在 DTO 中。
 *
 * @author C1ouD
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationConstants {

    /** 题库名称最大长度。 */
    public static final int QUESTION_BANK_TITLE_MAX = 200;

    /** 题库描述最大长度。 */
    public static final int QUESTION_BANK_DESCRIPTION_MAX = 2000;

    /** 题型标识最大长度。 */
    public static final int QUESTION_TYPE_MAX = 32;

    /** 题干最大长度。 */
    public static final int QUESTION_STEM_MAX = 8192;

    /** 选项 JSON 最大长度。 */
    public static final int QUESTION_OPTIONS_JSON_MAX = 65535;

    /** 答案 JSON 最大长度。 */
    public static final int QUESTION_ANSWER_JSON_MAX = 4096;

    /** 解析最大长度。 */
    public static final int QUESTION_ANALYSIS_MAX = 8192;

    /** 列表检索关键词最大长度。 */
    public static final int KEYWORD_MAX = 200;

    /** 分页：每页最大条数上限。 */
    public static final int PAGE_SIZE_MAX = 100;

    /** 文件导入上限（字节）：10 MB，超限拒绝。 */
    public static final long FILE_IMPORT_MAX_SIZE_BYTES = 10L * 1024 * 1024;
}
