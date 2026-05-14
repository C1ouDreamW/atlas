package cn.heycloudream.quiz_backend.service.ai;

import cn.heycloudream.quiz_backend.common.constants.QuizRedisCacheConstants;
import cn.heycloudream.quiz_backend.enums.AiImportTaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * 任务看门狗：定时扫描长期处于 PROCESSING 状态的任务，标记为 FAILED。
 * <p>
 * 使用 Redis 分布式锁（SETNX）确保单实例执行。
 * </p>
 *
 * @author atlas
 */
@SuppressWarnings("null")
@Slf4j
@Component
@RequiredArgsConstructor
public class AiImportTaskWatchdog {

    private static final String SCAN_PATTERN = "quiz:task:status:*";

    private final StringRedisTemplate stringRedisTemplate;
    private final AiImportTaskStatusStore statusStore;
    private final AiImportTaskMetaStore metaStore;

    /**
     * 任务超时阈值（毫秒），默认 10 分钟。
     */
    @Value("${quiz.ai-import.task-timeout-ms:600000}")
    private long taskTimeoutMs;

    /**
     * 每 2 分钟扫描一次。
     */
    @Scheduled(fixedDelay = 120_000)
    public void scan() {
        // 获取分布式锁，防止多实例并发扫描
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(QuizRedisCacheConstants.TASK_WATCHDOG_LOCK_KEY,
                        "1", Duration.ofSeconds(60));
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }

        try {
            Set<String> keys = stringRedisTemplate.keys(SCAN_PATTERN);
            if (keys == null || keys.isEmpty()) {
                return;
            }
            int failedCount = 0;
            for (String key : keys) {
                String taskId = key.substring("quiz:task:status:".length());
                try {
                    if (checkAndFailIfStale(taskId)) {
                        failedCount++;
                    }
                } catch (Exception e) {
                    log.warn("[Watchdog] 检查任务异常 taskId={}", taskId, e);
                }
            }
            if (failedCount > 0) {
                log.info("[Watchdog] 本轮标记超时任务 {} 个", failedCount);
            }
        } catch (Exception e) {
            log.error("[Watchdog] 扫描异常", e);
        }
    }

    /**
     * 检查单个任务是否超时，若超时则标记 FAILED。
     *
     * @return true 表示标记了 FAILED
     */
    private boolean checkAndFailIfStale(String taskId) {
        Optional<AiImportTaskStatus> current = statusStore.read(taskId)
                .map(vo -> {
                    try {
                        return AiImportTaskStatus.valueOf(vo.getStatus());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                });
        if (current.isEmpty() || current.get() != AiImportTaskStatus.PROCESSING) {
            return false;
        }

        // 检查元数据中的提交时间
        Optional<Long> submittedAt = metaStore.read(taskId)
                .map(meta -> meta.getSubmittedAt());

        if (submittedAt.isEmpty()) {
            // 无法获取提交时间，保守处理：不标记
            return false;
        }

        long elapsed = System.currentTimeMillis() - submittedAt.get();
        if (elapsed > taskTimeoutMs) {
            long elapsedMinutes = elapsed / 60_000;
            statusStore.markFailed(taskId,
                    "任务处理超时（已运行 " + elapsedMinutes + " 分钟，阈值 " + (taskTimeoutMs / 60_000) + " 分钟）");
            log.warn("[Watchdog] 任务超时已标记 FAILED taskId={} elapsedMinutes={}", taskId, elapsedMinutes);
            return true;
        }
        return false;
    }
}
