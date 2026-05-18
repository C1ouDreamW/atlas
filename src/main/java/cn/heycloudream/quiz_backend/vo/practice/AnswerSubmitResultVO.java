package cn.heycloudream.quiz_backend.vo.practice;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交答案判分结果 VO。
 * <p>
 * 客观题（单选/多选/判断）自动判分，{@code correct} 为 true/false；
 * 主观题或未知题型 {@code needsManualGrading=true}，{@code correct} 为 null。
 * </p>
 *
 * @author C1ouD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "提交答案判分结果")
public class AnswerSubmitResultVO {

    @Schema(description = "试题 ID", example = "50001")
    private Long questionId;

    @Schema(description = "是否正确；主观题时为 null", example = "true")
    private Boolean correct;

    @Schema(description = "是否需要人工判分（主观题或未知题型）", example = "false")
    private Boolean needsManualGrading;

    @Schema(
            description = "标准答案 JSON 数组字符串，如 [\"A\"]、[\"T\"]",
            example = "[\"A\",\"C\"]")
    private String answerJson;

    @Schema(description = "题目解析（可为空）")
    private String analysis;
}
