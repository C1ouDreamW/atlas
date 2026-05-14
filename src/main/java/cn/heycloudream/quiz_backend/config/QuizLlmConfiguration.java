package cn.heycloudream.quiz_backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用大模型相关配置属性绑定。
 *
 * @author C1ouD
 */
@Configuration
@EnableConfigurationProperties(QuizLlmProperties.class)
public class QuizLlmConfiguration {
}
