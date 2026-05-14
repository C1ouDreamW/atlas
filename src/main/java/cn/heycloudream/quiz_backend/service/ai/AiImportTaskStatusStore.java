package cn.heycloudream.quiz_backend.service.ai;

import cn.heycloudream.quiz_backend.common.constants.QuizRedisCacheConstants;
import cn.heycloudream.quiz_backend.enums.AiImportTaskStatus;
import cn.heycloudream.quiz_backend.vo.ai.AiImportTaskStatusVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * 管理 {@code quiz:task:status:{taskId}} 的读写。
 * <p>
 * 与旧 {@link AiQuestionImportRedisStatusStore} 共存，Phase C 完成后再切换。
 * </p>
 *
 * @author atlas
 */
@SuppressWarnings("null")
@Slf4j
@Component
@RequiredArgsConstructor
public class AiImportTaskStatusStore {

    private static final int MAX_MESSAGE_CHARS = 500;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 写入任务状态。
     */
    public void write(String taskId, AiImportTaskStatus status, String message, Integer totalCount) {
        AiImportTaskStatusVO vo = AiImportTaskStatusVO.builder()
                .taskId(taskId)
                .status(status.name())
                .message(truncate(message))
                .totalCount(totalCount)
                .questions(null) // 状态快照不携带预览题列表
                .build();
        write(taskId, vo);
    }

    /**
     * 写入完整状态快照（含预览题列表，仅 PARSED 态使用）。
     */
    public void writeFull(String taskId, AiImportTaskStatusVO vo) {
        write(taskId, vo);
    }

    /**
     * 读取任务状态。
     */
    public Optional<AiImportTaskStatusVO> read(String taskId) {
        String key = QuizRedisCacheConstants.taskStatusKey(taskId);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, AiImportTaskStatusVO.class));
        } catch (Exception e) {
            log.warn("[TaskId:{}] 读取任务状态 JSON 失败 key={}", taskId, key, e);
            return Optional.empty();
        }
    }

    /**
     * 将任务标记为终态 FAILED。
     */
    public void markFailed(String taskId, String reason) {
        write(taskId, AiImportTaskStatus.FAILED, reason, null);
    }

    private void write(String taskId, AiImportTaskStatusVO vo) {
        String key = QuizRedisCacheConstants.taskStatusKey(taskId);
        try {
            String json = objectMapper.writeValueAsString(vo);
            stringRedisTemplate.opsForValue().set(
                    key,
                    json,
                    Duration.ofSeconds(QuizRedisCacheConstants.TASK_STATUS_TTL_SECONDS));
        } catch (JsonProcessingException e) {
            log.error("[TaskId:{}] 序列化任务状态失败", taskId, e);
        }
    }

    private static String truncate(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        String t = message.trim();
        return t.length() <= MAX_MESSAGE_CHARS ? t : t.substring(0, MAX_MESSAGE_CHARS) + "...";
    }
}
