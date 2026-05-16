package cn.heycloudream.quiz_backend.service.ai;

import cn.heycloudream.quiz_backend.common.constants.QuizRedisCacheConstants;
import cn.heycloudream.quiz_backend.vo.ai.AiImportTaskMetaVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Redis Stream task dispatcher. Writes task metadata into the stream for the
 * external worker process to consume.
 *
 * @author atlas
 */
@SuppressWarnings("null")
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamTaskDispatcher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Ensure the stream and consumer group exist when the application starts.
     */
    @PostConstruct
    public void initStream() {
        try {
            stringRedisTemplate.execute((RedisCallback<Object>) connection -> {
                connection.streamCommands().xGroupCreate(
                        QuizRedisCacheConstants.TASK_STREAM_KEY.getBytes(StandardCharsets.UTF_8),
                        QuizRedisCacheConstants.TASK_STREAM_GROUP,
                        ReadOffset.latest(),
                        true);
                return null;
            });
            log.info("[Stream] consumer group created: {}", QuizRedisCacheConstants.TASK_STREAM_GROUP);
        } catch (Exception e) {
            if (isBusyGroup(e)) {
                log.info("[Stream] consumer group already exists: {}", QuizRedisCacheConstants.TASK_STREAM_GROUP);
                return;
            }
            log.warn("[Stream] consumer group initialization failed stream={} group={}",
                    QuizRedisCacheConstants.TASK_STREAM_KEY,
                    QuizRedisCacheConstants.TASK_STREAM_GROUP,
                    e);
        }
    }

    /**
     * Write task metadata to Redis Stream and return the generated entry ID.
     *
     * @param meta task metadata
     * @return Stream entry ID, such as 1684156800000-0
     */
    public String dispatch(AiImportTaskMetaVO meta) {
        Map<String, String> fields;
        try {
            String json = objectMapper.writeValueAsString(meta);
            fields = Collections.singletonMap("payload", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize task metadata", e);
        }

        var recordId = stringRedisTemplate.opsForStream().add(
                QuizRedisCacheConstants.TASK_STREAM_KEY,
                fields);
        String entryId = recordId.getValue();

        stringRedisTemplate.opsForStream().trim(
                QuizRedisCacheConstants.TASK_STREAM_KEY,
                QuizRedisCacheConstants.TASK_STREAM_MAX_LEN,
                true);

        log.info("[Stream] task dispatched entryId={} taskId={}", entryId, meta.getTaskId());
        return entryId;
    }

    private static boolean isBusyGroup(Throwable e) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("BUSYGROUP")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
