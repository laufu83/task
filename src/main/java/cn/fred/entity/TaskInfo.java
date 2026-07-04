package cn.fred.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskInfo {
    private Long id;
    private String taskName;
    private String cronExpr;
    private String httpUrl;
    private String requestMethod;
    private String requestBody;
    private String headerParams;
    private Integer taskStatus;
    private String remark;
    private LocalDateTime lastExecuteTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // 新增字段（需先执行 ALTER TABLE）
    private LocalDateTime nextRunTime;
    private Integer maxRetries;
    private Integer retryCount;
    private Integer timeoutSeconds;
    private Integer failCount;

}