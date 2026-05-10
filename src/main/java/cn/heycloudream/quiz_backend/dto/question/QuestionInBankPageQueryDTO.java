package cn.heycloudream.quiz_backend.dto.question;

import cn.heycloudream.quiz_backend.common.constants.ValidationConstants;
import cn.heycloudream.quiz_backend.common.dto.PageRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 指定题库下试题分页查询（题库 ID 由路径变量提供，故本 DTO 不包含 questionBankId）。
 *
 * @author atlas
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "指定题库下试题分页查询参数")
public class QuestionInBankPageQueryDTO extends PageRequestDTO {

    @Size(max = ValidationConstants.KEYWORD_MAX, message = "关键词过长")
    @Schema(description = "题干关键词模糊检索，可选")
    private String keyword;
}
