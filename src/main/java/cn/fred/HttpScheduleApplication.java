package cn.fred;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HttpScheduleApplication {
    public static void main(String[] args) {
        SpringApplication.run(HttpScheduleApplication.class, args);
    }
}