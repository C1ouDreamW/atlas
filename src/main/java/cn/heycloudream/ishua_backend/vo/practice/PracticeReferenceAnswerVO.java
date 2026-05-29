package cn.heycloudream.ishua_backend.vo.practice;

import cn.heycloudream.ishua_backend.enums.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简答题参考答案 VO，供用户主动「显示答案」时使用。
 *
 * @author C1ouD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "简答题参考答案（不含用户作答，不判分）")
public class PracticeReferenceAnswerVO {

    @Schema(description = "试题 ID", example = "50001")
    private Long questionId;

    @Schema(description = "题型", implementation = QuestionType.class, example = "SHORT_ANSWER")
    private String questionType;

    @Schema(
            description = "参考答案 JSON 数组字符串，每项为一个要点或段落，如 [\"要点一\",\"要点二\"]",
            example = "[\"客户端发 SYN，服务端回 SYN+ACK，客户端再发 ACK\"]")
    private String answerJson;

    @Schema(description = "题目解析（可为空）")
    private String analysis;
}
