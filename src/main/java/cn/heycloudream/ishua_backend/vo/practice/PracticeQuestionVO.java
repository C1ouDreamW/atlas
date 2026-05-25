package cn.heycloudream.ishua_backend.vo.practice;

import cn.heycloudream.ishua_backend.enums.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷题模式试题展示 VO，故意隐藏 answerJson 与 analysis，防止用户在提交前获取答案。
 * 判分结果通过 {@link AnswerSubmitResultVO} 返回。
 *
 * @author C1ouD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "刷题模式试题（不含答案与解析）")
public class PracticeQuestionVO {

    @Schema(description = "试题主键", example = "50001")
    private Long id;

    @Schema(description = "所属题库 ID", example = "1001")
    private Long questionBankId;

    @Schema(description = "题型", implementation = QuestionType.class, example = "SINGLE")
    private String questionType;

    @Schema(description = "题干纯文本")
    private String stem;

    @Schema(
            description = "选项 JSON 数组字符串，如 [\"TCP\",\"UDP\"]；判断题为 []",
            example = "[\"TCP\",\"UDP\",\"IP\",\"ICMP\"]")
    private String optionsJson;

    @Schema(description = "题库内排序号", example = "1")
    private Integer sortNo;
}
