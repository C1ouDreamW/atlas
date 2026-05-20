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
 * @author C1ouD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通用分页响应（total + records）。单条类型见各接口 200 响应中 data.records 的 items")
public class PageResultVO<T> {

    /**
     * 总记录数。
     */
    @Schema(description = "总记录数", example = "100")
    private Long total;

    /**
     * 当前页数据列表。
     */
    @Schema(description = "当前页数据列表（字段名为 records，非 list/items）")
    private List<T> records;
}
