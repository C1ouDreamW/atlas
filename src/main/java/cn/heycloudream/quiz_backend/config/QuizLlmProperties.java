package cn.heycloudream.quiz_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 大模型 HTTP 调用相关配置（OpenAI 兼容 Chat Completions）。
 *
 * @author atlas
 */
@Data
@ConfigurationProperties(prefix = "quiz.llm")
public class QuizLlmProperties {

    /**
     * 服务根地址，例如 https://api.deepseek.com（不含路径）。
     */
    private String baseUrl = "https://api.deepseek.com";

    /**
     * API Key，建议通过环境变量注入，勿提交到仓库。
     */
    private String apiKey = "";

    /**
     * 模型名称。
     */
    private String model = "deepseek-chat";

    /**
     * Chat Completions 路径（相对 baseUrl）。
     */
    private String chatPath = "/v1/chat/completions";

    /**
     * 连接超时（毫秒）。
     */
    private int connectTimeoutMs = 30_000;

    /**
     * 读超时（毫秒），长文档解析可适当放大。
     */
    private int readTimeoutMs = 300_000;
}
