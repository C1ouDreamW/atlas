package cn.heycloudream.quiz_test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Web 切片测试引导类：位于 {@code quiz_test} 包，避免拉起主应用 {@code @MapperScan}。
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class QuizTestApplication {
}
