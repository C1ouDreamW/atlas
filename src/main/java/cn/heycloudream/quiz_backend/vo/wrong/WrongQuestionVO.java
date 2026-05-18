package cn.heycloudream.quiz_backend.vo.wrong;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 错题本记录展示 VO，聚合错题元信息与题目摘要，不暴露答案。
 *
 * @author C1ouD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "错题本记录")
public class WrongQuestionVO {

    @Schema(description = "错题本记录 ID（用于移除操作）", example = "1")
    private Long id;

    @Schema(description = "试题 ID", example = "50001")
    private Long questionId;

    @Schema(description = "所属题库 ID", example = "1001")
    private Long questionBankId;

    @Schema(description = "题型编码（SINGLE / MULTI / JUDGE）", example = "SINGLE")
    private String questionType;

    @Schema(description = "题干纯文本")
    private String stem;

    @Schema(
            description = "选项 JSON 数组字符串",
            example = "[\"TCP\",\"UDP\",\"IP\",\"ICMP\"]")
    private String optionsJson;

    @Schema(description = "累计做错次数", example = "3")
    private Integer wrongCount;

    @Schema(description = "最近一次做错时间", type = "string", format = "date-time", example = "2026-05-19T00:00:00")
    private LocalDateTime lastWrongTime;
}
