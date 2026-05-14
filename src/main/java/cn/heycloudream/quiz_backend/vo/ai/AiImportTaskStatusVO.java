package cn.heycloudream.quiz_backend.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 导入任务状态快照（新任务体系）。
 *
 * @author atlas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 导入任务状态快照")
public class AiImportTaskStatusVO {

    @Schema(description = "任务 ID（UUID）")
    private String taskId;

    @Schema(description = "SUBMITTED / PROCESSING / PARSED / IMPORTING / IMPORTED / FAILED")
    private String status;

    @Schema(description = "错误信息或业务说明（成功时可为空）")
    private String message;

    @Schema(description = "解析出的题目总数（PARSED 态及之后有意义）")
    private Integer totalCount;

    @Schema(description = "预览题目列表（仅 PARSED 态有数据）")
    private List<QuestionPreviewVO> questions;
}
