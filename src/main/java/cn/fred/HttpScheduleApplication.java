package cn.fred;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync  // 添加这个注解
public class HttpScheduleApplication {
    public static void main(String[] args) {
        SpringApplication.run(HttpScheduleApplication.class, args);
    }
}