package cn.heycloudream.quiz_backend.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 全局通用分页响应包装。
 *
 * @param <T> 单条记录类型
 * @author atlas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通用分页响应")
public class PageResultVO<T> {

    /**
     * 总记录数。
     */
    @Schema(description = "总记录数", example = "100")
    private Long total;

    /**
     * 当前页数据列表。
     */
    @Schema(description = "当前页数据列表")
    private List<T> records;
}
