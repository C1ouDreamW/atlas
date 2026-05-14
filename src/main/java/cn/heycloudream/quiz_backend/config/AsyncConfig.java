package cn.heycloudream.quiz_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务与线程池配置（智能导入等耗时任务）。
 *
 * @author C1ouD
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 大模型解析与批量落库专用线程池，避免阻塞 Web 请求线程。
     */
    public static final String AI_IMPORT_EXECUTOR = "aiImportExecutor";

    @Value("${quiz.async.ai-import.core-pool-size:2}")
    private int corePoolSize;

    @Value("${quiz.async.ai-import.max-pool-size:4}")
    private int maxPoolSize;

    @Value("${quiz.async.ai-import.queue-capacity:200}")
    private int queueCapacity;

    @Bean(name = AI_IMPORT_EXECUTOR)
    public Executor aiImportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("ai-import-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
