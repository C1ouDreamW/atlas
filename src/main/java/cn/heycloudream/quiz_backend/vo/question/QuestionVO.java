package cn.heycloudream.quiz_backend.vo.question;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 试题对外展示数据（列表与详情共用），不包含大模型原始 JSON 等内部字段。
 *
 * @author atlas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "试题信息")
public class QuestionVO {

    @Schema(description = "试题主键", example = "50001")
    private Long id;

    @Schema(description = "所属题库 ID", example = "1001")
    private Long questionBankId;

    @Schema(description = "题型编码", example = "SINGLE")
    private String questionType;

    @Schema(description = "题干纯文本")
    private String stem;

    @Schema(description = "选项等结构化数据（JSON 字符串）")
    private String optionsJson;

    @Schema(description = "标准答案（JSON 字符串）")
    private String answerJson;

    @Schema(description = "题目解析")
    private String analysis;

    @Schema(description = "题库内排序号", example = "1")
    private Integer sortNo;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
