package cn.heycloudream.quiz_backend.common.dto;

import cn.heycloudream.quiz_backend.common.constants.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局通用分页请求参数（页码 + 每页条数）。
 *
 * @author atlas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通用分页请求参数")
public class PageRequestDTO {

    /**
     * 当前页码，从 1 开始。
     */
    @NotNull(message = "当前页码不能为空")
    @Min(value = 1, message = "当前页码至少为 1")
    @Schema(description = "当前页码，从 1 开始", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer current;

    /**
     * 每页条数。
     */
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数至少为 1")
    @Max(value = ValidationConstants.PAGE_SIZE_MAX, message = "每页条数超出上限")
    @Schema(description = "每页条数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pageSize;
}
