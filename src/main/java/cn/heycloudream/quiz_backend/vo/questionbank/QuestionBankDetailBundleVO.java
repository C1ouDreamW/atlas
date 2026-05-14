package cn.heycloudream.quiz_backend.vo.questionbank;

import cn.heycloudream.quiz_backend.vo.question.QuestionVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 公开热点题库刷题用聚合视图：题库信息 + 全量试题列表（与 Redis 缓存 JSON 结构一致）。
 *
 * @author C1ouD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "热点题库详情（含全量试题）")
public class QuestionBankDetailBundleVO {

    @Schema(description = "题库基本信息")
    private QuestionBankVO bank;

    @Schema(description = "该题库下全部试题（有序）")
    private List<QuestionVO> questions;
}
