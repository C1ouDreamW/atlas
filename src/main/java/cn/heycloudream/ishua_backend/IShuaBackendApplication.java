package cn.heycloudream.ishua_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("cn.heycloudream.ishua_backend.mapper")
public class IShuaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(IShuaBackendApplication.class, args);
	}

}
