package cn.heycloudream.quiz_backend.support;

import cn.heycloudream.quiz_backend.service.ai.AiImportTaskWatchdog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 缓存专项集成测试基类：Testcontainers 启动真实 Redis。
 * <p>
 * 不 Mock {@code StringRedisTemplate}；本机无 Docker 时自动跳过（{@code disabledWithoutDocker}）。
 * 全量 {@code mvn test} 默认仍使用 {@link AbstractMockRedisSpringBootTest}。
 * </p>
 */
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class RedisTestcontainersSupport {

    @MockBean
    protected AiImportTaskWatchdog aiImportTaskWatchdog;

    @SuppressWarnings("resource")
    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379).toString());
        registry.add("spring.data.redis.password", () -> "");
    }

    @BeforeEach
    void clearUserContextBeforeEach() {
        UserContextTestSupport.clear();
    }

    @AfterEach
    void clearUserContextAfterEach() {
        UserContextTestSupport.clear();
    }
}
