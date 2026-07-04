package cn.fred.entity;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskExecuteLog {
    private Long id;
    private Long taskId;
    private String taskName;
    private Integer executeStatus; // 1成功 0失败
    private String requestMethod;
    private String requestUrl;
    private String requestHeader;
    private String requestBody;
    private String responseData;
    private String errorMsg;
    private Long costTime;
    private LocalDateTime executeTime;
}