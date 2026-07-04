package cn.fred.dao;

import cn.fred.common.BaseDao;
import cn.fred.common.PageResult;
import cn.fred.entity.TaskInfo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class TaskInfoDao extends BaseDao<TaskInfo> {

    public TaskInfoDao() {
        super(TaskInfo.class);
    }

    // ==================== CRUD ====================

    public PageResult<TaskInfo> pageTask(long pageNum, long pageSize) {
        String countSql = "SELECT COUNT(*) FROM task_info";
        String listSql = "SELECT * FROM task_info ORDER BY create_time DESC LIMIT ?,?";
        return page(pageNum, pageSize, countSql, listSql);
    }

    public TaskInfo getById(Long id) {
        return findById("SELECT * FROM task_info WHERE id = ?", id);
    }

    public int insertTask(TaskInfo task) {
        String sql = "INSERT INTO task_info(task_name, cron_expr, http_url, request_method, request_body, header_params, task_status, remark) VALUES (?,?,?,?,?,?,?,?)";
        Long id = insert(sql, task.getTaskName(), task.getCronExpr(), task.getHttpUrl(),
                task.getRequestMethod(), task.getRequestBody(), task.getHeaderParams(),
                task.getTaskStatus(), task.getRemark());
        return id != null ? id.intValue() : 0;
    }

    public int updateTask(TaskInfo task) {
        String sql = "UPDATE task_info SET task_name=?, cron_expr=?, http_url=?, request_method=?, request_body=?, header_params=?, task_status=?, remark=?, last_execute_time=NOW() WHERE id=?";
        return update(sql, task.getTaskName(), task.getCronExpr(), task.getHttpUrl(),
                task.getRequestMethod(), task.getRequestBody(), task.getHeaderParams(),
                task.getTaskStatus(), task.getRemark(), task.getId());
    }

    public int deleteById(Long id) {
        return delete("DELETE FROM task_info WHERE id = ?", id);
    }

    // ==================== 调度任务专用 ====================

    public List<TaskInfo> listAllEnable() {
        return list("SELECT * FROM task_info WHERE task_status = 1");
    }

    public List<TaskInfo> findTasksToExecute(LocalDateTime now) {
        String sql = "SELECT * FROM task_info WHERE task_status = 1 AND (next_run_time IS NULL OR next_run_time <= ?)";
        return list(sql, now);
    }

    public int updateNextRunTime(Long taskId, LocalDateTime nextRunTime, LocalDateTime lastExecuteTime) {
        String sql = "UPDATE task_info SET next_run_time = ?, last_execute_time = ?, retry_count = 0 WHERE id = ?";
        return update(sql, nextRunTime, lastExecuteTime, taskId);
    }

    public int updateTaskStatus(Long taskId, Integer status) {
        String sql = "UPDATE task_info SET task_status = ? WHERE id = ?";
        return update(sql, status, taskId);
    }

    public int incrementRetryCount(Long taskId) {
        String sql = "UPDATE task_info SET retry_count = retry_count + 1 WHERE id = ?";
        return update(sql, taskId);
    }

    public List<TaskInfo> findAllOrderByNextRun() {
        String sql = "SELECT * FROM task_info ORDER BY next_run_time ASC";
        return list(sql);
    }

    public int increaseFailCount(Long taskId) {
        String sql = "UPDATE task_info SET fail_count = fail_count + 1 WHERE id = ?";
        update(sql, taskId);
        // 查询当前最新失败次数
        return getById(taskId).getFailCount();
    }
    public void clearFailCount(Long taskId) {
        String sql = "UPDATE task_info SET fail_count = 0 WHERE id = ?";
        update(sql, taskId);
    }
 }