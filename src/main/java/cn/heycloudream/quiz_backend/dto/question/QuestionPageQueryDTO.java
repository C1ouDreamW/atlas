package cn.heycloudream.quiz_backend.dto.question;

import cn.heycloudream.quiz_backend.common.constants.ValidationConstants;
import cn.heycloudream.quiz_backend.common.dto.PageRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 某题库下试题分页查询条件，继承通用分页参数。
 *
 * @author atlas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "试题分页查询请求")
public class QuestionPageQueryDTO extends PageRequestDTO {

    @NotNull(message = "所属题库 ID 不能为空")
    @Schema(description = "所属题库 ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long questionBankId;

    @Size(max = ValidationConstants.KEYWORD_MAX, message = "关键词过长")
    @Schema(description = "题干关键词模糊检索，可选")
    private String keyword;
}
