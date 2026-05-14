package cn.heycloudream.quiz_backend.service.ai;

import cn.heycloudream.quiz_backend.common.constants.QuizRedisCacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 批量导入幂等服务：通过 Redis SETNX 锁防止同一 taskId 重复落库。
 *
 * @author atlas
 */
@SuppressWarnings("null")
@Slf4j
@Component
@RequiredArgsConstructor
public class ImportIdempotentService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取导入锁，同一 taskId 只允许落库一次。
     *
     * @param taskId 任务 ID
     * @return true 表示首次导入，允许继续；false 表示已导入过
     */
    public boolean tryAcquire(String taskId) {
        String key = QuizRedisCacheConstants.taskImportLockKey(taskId);
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(QuizRedisCacheConstants.TASK_IMPORT_LOCK_TTL_SECONDS));
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 检查是否已导入过。
     */
    public boolean isAlreadyImported(String taskId) {
        String key = QuizRedisCacheConstants.taskImportLockKey(taskId);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }
}
