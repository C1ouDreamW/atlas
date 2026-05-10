package cn.heycloudream.quiz_backend.dto.question;

import cn.heycloudream.quiz_backend.common.constants.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全量更新试题请求体（PUT），字段与 {@link QuestionCreateDTO} 一致但不包含 questionBankId（不允许更换所属题库），主键 id 由路径传递。
 *
 * @author atlas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "全量更新试题请求")
public class QuestionUpdateDTO {

    @NotBlank(message = "题型不能为空")
    @Size(max = ValidationConstants.QUESTION_TYPE_MAX, message = "题型标识过长")
    @Schema(description = "题型编码，如 SINGLE、MULTI、JUDGE", example = "SINGLE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String questionType;

    @NotBlank(message = "题干不能为空")
    @Size(max = ValidationConstants.QUESTION_STEM_MAX, message = "题干过长")
    @Schema(description = "题干纯文本", requiredMode = Schema.RequiredMode.REQUIRED)
    private String stem;

    @NotBlank(message = "选项 JSON 不能为空")
    @Size(max = ValidationConstants.QUESTION_OPTIONS_JSON_MAX, message = "选项 JSON 过长")
    @Schema(description = "选项等结构化数据（JSON 字符串）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String optionsJson;

    @NotBlank(message = "答案 JSON 不能为空")
    @Size(max = ValidationConstants.QUESTION_ANSWER_JSON_MAX, message = "答案 JSON 过长")
    @Schema(description = "标准答案（JSON 字符串）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String answerJson;

    @Size(max = ValidationConstants.QUESTION_ANALYSIS_MAX, message = "解析过长")
    @Schema(description = "题目解析，可选")
    private String analysis;

    @Min(value = 1, message = "排序号至少为 1")
    @Schema(description = "题库内排序号，可选；不传则由服务端默认")
    private Integer sortNo;
}
