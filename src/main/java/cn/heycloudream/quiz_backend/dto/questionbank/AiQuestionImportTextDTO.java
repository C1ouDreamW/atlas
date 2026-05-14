package cn.heycloudream.quiz_backend.dto.questionbank;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 智能导入请求体：从文档抽取后的纯文本。
 *
 * @author C1ouD
 */
@Data
@Schema(description = "智能导入纯文本")
public class AiQuestionImportTextDTO {

    @NotBlank(message = "导入纯文本不能为空")
    @Schema(description = "待解析的题库纯文本", requiredMode = Schema.RequiredMode.REQUIRED)
    private String plainText;
}
