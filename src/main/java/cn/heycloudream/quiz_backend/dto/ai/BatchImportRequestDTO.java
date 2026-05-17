package cn.heycloudream.quiz_backend.dto.ai;

import cn.heycloudream.quiz_backend.vo.ai.QuestionPreviewVO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量确认导入请求：前端预览确认后提交。
 *
 * @author atlas
 */
@Data
@Schema(description = "批量确认导入请求")
public class BatchImportRequestDTO {

    @NotBlank(message = "任务 ID 不能为空")
    @Schema(description = "AI 提交/轮询接口返回的任务 ID（UUID）", example = "a1b2c3d4e5f67890abcdef1234567890")
    private String taskId;

    @NotEmpty(message = "导入题目列表不能为空")
    @Valid
    @Schema(description = "经用户确认/编辑后的题目列表")
    private List<QuestionPreviewVO> questions;
}
