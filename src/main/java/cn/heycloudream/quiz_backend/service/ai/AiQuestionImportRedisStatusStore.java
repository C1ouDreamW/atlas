package cn.heycloudream.quiz_backend.service.ai;

import cn.heycloudream.quiz_backend.common.constants.QuizRedisCacheConstants;
import cn.heycloudream.quiz_backend.enums.AiQuestionImportRunStatus;
import cn.heycloudream.quiz_backend.vo.ai.AiImportStatusVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * 将智能导入异步状态写入 Redis，供前端经 HTTP 轮询查询。
 *
 * @author C1ouD
 */
@SuppressWarnings("null")
@Slf4j
@Component
@RequiredArgsConstructor
public class AiQuestionImportRedisStatusStore {

    private static final int MAX_MESSAGE_CHARS = 500;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 标记任务进行中。
     */
    public void writeProcessing(long bankId) {
        write(bankId, AiImportStatusVO.builder()
                .status(AiQuestionImportRunStatus.PROCESSING.name())
                .message(null)
                .savedCount(null)
                .build());
    }

    /**
     * 标记任务成功结束。
     *
     * @param savedCount 落库条数，允许为 0
     */
    public void writeSuccess(long bankId, int savedCount, String message) {
        write(bankId, AiImportStatusVO.builder()
                .status(AiQuestionImportRunStatus.SUCCESS.name())
                .message(truncateMessage(message))
                .savedCount(savedCount)
                .build());
    }

    /**
     * 标记任务失败。
     */
    public void writeFailed(long bankId, String reason) {
        write(bankId, AiImportStatusVO.builder()
                .status(AiQuestionImportRunStatus.FAILED.name())
                .message(truncateMessage(reason))
                .savedCount(null)
                .build());
    }

    /**
     * 读取当前状态；Key 不存在或反序列化失败时返回 empty。
     */
    public Optional<AiImportStatusVO> read(long bankId) {
        String key = QuizRedisCacheConstants.importStatusKey(bankId);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, AiImportStatusVO.class));
        } catch (Exception e) {
            log.warn("[BankId:{}] 读取导入状态 JSON 失败，将视为无状态 key={}", bankId, key, e);
            return Optional.empty();
        }
    }

    private void write(long bankId, AiImportStatusVO vo) {
        String key = QuizRedisCacheConstants.importStatusKey(bankId);
        try {
            String json = objectMapper.writeValueAsString(vo);
            stringRedisTemplate.opsForValue().set(
                    key,
                    json,
                    Duration.ofSeconds(QuizRedisCacheConstants.AI_IMPORT_STATUS_TTL_SECONDS));
        } catch (JsonProcessingException e) {
            log.error("[BankId:{}] 序列化导入状态失败", bankId, e);
        }
    }

    private static String truncateMessage(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        String t = message.trim();
        if (t.length() <= MAX_MESSAGE_CHARS) {
            return t;
        }
        return t.substring(0, MAX_MESSAGE_CHARS) + "...";
    }
}
