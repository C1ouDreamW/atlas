package cn.heycloudream.ishua_backend.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import cn.heycloudream.ishua_backend.service.ai.AiImportTaskWatchdog;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 默认集成测试基类：H2 内存库 + Mock Redis（不依赖本机 MySQL/Redis）。
 * <p>
 * 需要验证 Cache-Aside 真 Redis 行为的用例请改用 {@link RedisTestcontainersSupport}。
 * </p>
 */
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@ActiveProfiles("test")
public abstract class AbstractMockRedisSpringBootTest {

    @MockBean
    protected StringRedisTemplate stringRedisTemplate;

    /** 主类 {@code @EnableScheduling} 会忽略 {@code spring.task.scheduling.enabled}，测试侧 Mock 定时任务。 */
    @MockBean
    protected AiImportTaskWatchdog aiImportTaskWatchdog;

    @BeforeEach
    void clearUserContextBeforeEach() {
        UserContextTestSupport.clear();
    }

    @AfterEach
    void clearUserContextAfterEach() {
        UserContextTestSupport.clear();
    }
}
