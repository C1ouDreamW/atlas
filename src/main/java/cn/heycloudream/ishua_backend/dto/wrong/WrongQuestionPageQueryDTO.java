package cn.heycloudream.ishua_backend.dto.wrong;

import cn.heycloudream.ishua_backend.common.dto.PageRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 错题本分页查询请求，继承通用分页参数。
 *
 * @author C1ouD
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "错题本分页查询参数")
public class WrongQuestionPageQueryDTO extends PageRequestDTO {

    @Schema(description = "按题库 ID 过滤，不传则查询全部错题", example = "1001")
    private Long bankId;
}
