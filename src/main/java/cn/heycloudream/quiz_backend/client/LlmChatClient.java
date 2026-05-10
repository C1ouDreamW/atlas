package cn.heycloudream.quiz_backend.client;

import cn.heycloudream.quiz_backend.config.QuizLlmProperties;
import cn.heycloudream.quiz_backend.exception.LlmInvokeException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 Chat Completions 调用客户端，带连接/读超时。
 *
 * @author atlas
 */
@SuppressWarnings("null")
@Slf4j
@Component
public class LlmChatClient {

    private final RestClient restClient;
    private final QuizLlmProperties properties;
    private final ObjectMapper objectMapper;

    public LlmChatClient(QuizLlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 调用大模型，返回助手消息中的纯文本（应为 JSON 数组字符串）。
     *
     * @param systemPrompt 系统提示词
     * @param userPlainText 用户侧杂乱题库纯文本
     * @return 模型输出的 content 字符串
     */
    public String chatCompletion(String systemPrompt, String userPlainText) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new LlmInvokeException("未配置 quiz.llm.api-key，无法调用大模型");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("temperature", 0.2);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPlainText)
        ));
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new LlmInvokeException("构造大模型请求体失败", e);
        }
        try {
            String raw = restClient.post()
                    .uri(properties.getChatPath())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey().trim())
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);
            if (raw == null || raw.isBlank()) {
                throw new LlmInvokeException("大模型返回空响应体");
            }
            JsonNode root = objectMapper.readTree(raw);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (!contentNode.isTextual() || contentNode.asText().isBlank()) {
                throw new LlmInvokeException("大模型响应中缺少 choices[0].message.content 文本");
            }
            return contentNode.asText();
        } catch (RestClientResponseException e) {
            log.error("大模型 HTTP 调用失败 status={} body={}", e.getStatusCode().value(),
                    truncate(e.getResponseBodyAsString(), 2000));
            throw new LlmInvokeException("大模型 HTTP 调用失败: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            log.error("大模型连接/读超时或网络不可达", e);
            throw new LlmInvokeException("大模型请求超时或网络不可达", e);
        } catch (LlmInvokeException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmInvokeException("解析大模型响应失败", e);
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
