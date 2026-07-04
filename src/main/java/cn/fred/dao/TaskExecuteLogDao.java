package cn.fred.dao;

import cn.fred.common.BaseDao;
import cn.fred.common.PageResult;
import cn.fred.entity.TaskExecuteLog;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TaskExecuteLogDao extends BaseDao<TaskExecuteLog> {

    public TaskExecuteLogDao() {
        super(TaskExecuteLog.class);
    }

    // ==================== CRUD ====================

    public PageResult<TaskExecuteLog> pageLog(Long pageNum, Long pageSize, Long taskId) {
        if (taskId != null && taskId > 0) {
            String countSql = "SELECT COUNT(*) FROM task_execute_log WHERE task_id = ?";
            String listSql = "SELECT * FROM task_execute_log WHERE task_id = ? ORDER BY execute_time DESC LIMIT ?,?";
            return page(pageNum, pageSize, countSql, listSql, taskId);
        } else {
            String countSql = "SELECT COUNT(*) FROM task_execute_log";
            String listSql = "SELECT * FROM task_execute_log ORDER BY execute_time DESC LIMIT ?,?";
            return page(pageNum, pageSize, countSql, listSql);
        }
    }

    public int saveLog(TaskExecuteLog log) {
        String sql = "INSERT INTO task_execute_log(task_id, task_name, execute_status, request_method, request_url, request_header, request_body, response_data, error_msg, cost_time) VALUES (?,?,?,?,?,?,?,?,?,?)";
        Long id = insert(sql, log.getTaskId(), log.getTaskName(), log.getExecuteStatus(),
                log.getRequestMethod(), log.getRequestUrl(), log.getRequestHeader(),
                log.getRequestBody(), log.getResponseData(), log.getErrorMsg(), log.getCostTime());
        return id != null ? id.intValue() : 0;
    }

    public int clearExpireLog() {
        return delete("DELETE FROM task_execute_log WHERE execute_time < DATE_SUB(NOW(), INTERVAL 30 DAY)");
    }

    public TaskExecuteLog getById(Long id) {
        return findById("SELECT * FROM task_execute_log WHERE id = ?", id);
    }
    public int deleteById(Long id) {
        return delete("DELETE FROM task_execute_log WHERE id = ?", id);
    }

    public int clearByTaskId(Long taskId) {
        return delete("DELETE FROM task_execute_log WHERE task_id = ?", taskId);
    }

    // ==================== 扩展方法 ====================

    public int clearLogsOlderThanDays(int days) {
        String sql = "DELETE FROM task_execute_log WHERE execute_time < DATE_SUB(NOW(), INTERVAL ? DAY)";
        return update(sql, days);
    }

    public List<TaskExecuteLog> findLatest(int limit) {
        String sql = "SELECT * FROM task_execute_log ORDER BY execute_time DESC LIMIT ?";
        return list(sql, limit);
    }

    public long countByTaskId(Long taskId) {
        return count("SELECT COUNT(*) FROM task_execute_log WHERE task_id = ?", taskId);
    }
}