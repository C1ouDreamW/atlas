package cn.heycloudream.quiz_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class QuizBackendApplicationTests {

	/**
	 * 单元测试环境未必启动 Redis，Mock 以避免 Lettuce 连接失败导致上下文无法加载。
	 */
	@MockBean
	private StringRedisTemplate stringRedisTemplate;

	@Test
	void contextLoads() {
	}

}
