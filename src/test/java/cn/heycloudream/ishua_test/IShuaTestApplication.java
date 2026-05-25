package cn.heycloudream.ishua_test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Web 切片测试引导类：位于 {@code ishua_test} 包，避免拉起主应用 {@code @MapperScan}。
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class IShuaTestApplication {
}
