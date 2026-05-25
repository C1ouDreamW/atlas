package cn.heycloudream.ishua_backend.dto.questionbank;

import cn.heycloudream.ishua_backend.common.constants.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建题库请求体（POST）。
 *
 * @author C1ouD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建题库请求")
public class QuestionBankCreateDTO {

    @NotBlank(message = "题库名称不能为空")
    @Size(max = ValidationConstants.QUESTION_BANK_TITLE_MAX, message = "题库名称过长")
    @Schema(description = "题库名称", example = "2026计算机网络期末必刷题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Size(max = ValidationConstants.QUESTION_BANK_DESCRIPTION_MAX, message = "题库描述过长")
    @Schema(description = "题库描述", example = "面向期末周的重点题型整理")
    private String description;

    @NotNull(message = "是否公开不能为空")
    @Min(value = 0, message = "是否公开取值非法")
    @Max(value = 1, message = "是否公开取值非法")
    @Schema(description = "是否公开：0-否，1-是", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer isPublic;
}
