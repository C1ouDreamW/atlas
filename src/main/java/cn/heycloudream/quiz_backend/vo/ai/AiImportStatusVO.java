package cn.heycloudream.quiz_backend.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能导入任务状态快照（Redis 存 JSON，与轮询接口返回结构一致）。
 *
 * @author C1ouD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "智能导入异步任务状态")
public class AiImportStatusVO {

    @Schema(description = "PROCESSING / SUCCESS / FAILED", example = "SUCCESS")
    private String status;

    @Schema(description = "失败原因或业务说明（成功时可为空）")
    private String message;

    @Schema(description = "成功落库的试题条数；进行中或非成功态可为 null")
    private Integer savedCount;
}
