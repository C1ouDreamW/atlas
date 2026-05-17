package cn.heycloudream.quiz_backend.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 预览题目 VO，供前端渲染导入确认页。
 * <p>
 * 与正式落库的 {@code Question} 实体分离，方便前端增删改后再提交。
 * </p>
 *
 * @author atlas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = """
        AI 预览/批量确认题目（确认导入前可编辑）。
        与 QuestionVO 不同：此处 options、answer 为数组；入库后读接口为 optionsJson、answerJson 字符串。
        """)
public class QuestionPreviewVO {

    @Schema(description = "题型：SINGLE | MULTI | JUDGE", example = "SINGLE")
    private String questionType;

    @Schema(description = "题干文本", example = "理想气体状态方程 PV=nRT 中，R 的数值是？")
    private String stem;

    @Schema(description = "选项文案列表（判断题固定 [\"正确\",\"错误\"]）")
    private List<String> options;

    @Schema(
            description = "正确答案列表：单选 [\"B\"]；多选 [\"A\",\"C\"]；判断题 [\"T\"] 或 [\"F\"]",
            example = "[\"C\"]")
    private List<String> answer;

    @Schema(description = "题目解析")
    private String analysis;
}
