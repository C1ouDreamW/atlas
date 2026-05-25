package cn.heycloudream.ishua_backend.dto.practice;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 提交单题答案请求体。
 *
 * @author C1ouD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "提交答案请求")
public class AnswerSubmitDTO {

    @NotNull(message = "用户答案不能为空")
    @ArraySchema(
            arraySchema = @Schema(
                    description = "用户选择的答案列表。单选/多选传大写字母；判断题传 T/F",
                    requiredMode = Schema.RequiredMode.REQUIRED),
            schema = @Schema(type = "string", example = "A", description = "选项字母或 T/F"))
    private List<String> userAnswer;
}
