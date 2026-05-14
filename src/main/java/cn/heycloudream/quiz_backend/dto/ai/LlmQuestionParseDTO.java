package cn.heycloudream.quiz_backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 大模型返回的 JSON 数组元素结构，用于 Jackson 反序列化。
 *
 * @author C1ouD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmQuestionParseDTO {

    private String questionType;

    private String stem;

    private List<String> options;

    private List<String> answer;

    private String analysis;
}
