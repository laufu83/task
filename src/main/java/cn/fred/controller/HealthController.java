package cn.fred.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    /**
     * 基础健康检查 - 返回 "ok"
     * 适用于 Back4app 等平台的存活探针
     */
    @GetMapping
    public String health() {
        return "ok";
    }

    /**
     * 详细健康检查 - 返回 JSON 格式的完整状态
     * 包含：状态、时间戳、数据库连接状态
     */
    @GetMapping("/detail")
    public Map<String, Object> healthDetail() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        result.put("service", "http-task-scheduler");

        // 检查数据库连接
        boolean dbOk = checkDatabase();
        result.put("database", dbOk ? "UP" : "DOWN");

        return result;
    }

    /**
     * 仅检查数据库连接状态
     */
    @GetMapping("/db")
    public Map<String, Object> dbHealth() {
        Map<String, Object> result = new HashMap<>();
        boolean dbOk = checkDatabase();
        result.put("database", dbOk ? "UP" : "DOWN");
        result.put("status", dbOk ? "UP" : "DOWN");
        return result;
    }

    /**
     * 检查数据库连接是否正常
     */
    private boolean checkDatabase() {
        if (jdbcTemplate == null) {
            return false;
        }
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}