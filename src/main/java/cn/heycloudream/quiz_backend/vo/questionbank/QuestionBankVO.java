package cn.heycloudream.quiz_backend.vo.questionbank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 题库对外展示数据（列表与详情共用）。
 *
 * @author C1ouD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "题库信息")
public class QuestionBankVO {

    @Schema(description = "题库主键", example = "1001")
    private Long id;

    @Schema(description = "创建者用户 ID", example = "20")
    private Long userId;

    @Schema(description = "题库名称", example = "2026计算机网络期末必刷题")
    private String title;

    @Schema(description = "题库描述")
    private String description;

    @Schema(description = "是否公开：0=私有，1=公开（公开题库可出现在大厅与热点刷题接口）", example = "1")
    private Integer isPublic;

    @Schema(description = "创建时间", type = "string", format = "date-time")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", type = "string", format = "date-time")
    private LocalDateTime updateTime;
}
