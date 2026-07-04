package cn.fred.service;

import cn.fred.common.PageResult;
import cn.fred.entity.TaskExecuteLog;
import cn.fred.dao.TaskExecuteLogDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskExecuteLogService {

    @Autowired
    private TaskExecuteLogDao taskExecuteLogDao;

    // ==================== 基础 CRUD ====================

    public PageResult<TaskExecuteLog> page(Long pageNum, Long pageSize, Long taskId) {
        return taskExecuteLogDao.pageLog(pageNum, pageSize, taskId);
    }

    public TaskExecuteLog getById(Long id) {
        return taskExecuteLogDao.getById(id);
    }

    @Transactional
    public void save(TaskExecuteLog log) {
        taskExecuteLogDao.saveLog(log);
    }

    /**
     * 删除单条日志
     */
    @Transactional
    public boolean deleteById(Long id) {
        return taskExecuteLogDao.deleteById(id) > 0;
    }

    /**
     * 清空指定任务的所有日志
     */
    @Transactional
    public int clearByTaskId(Long taskId) {
        return taskExecuteLogDao.clearByTaskId(taskId);
    }

    /**
     * 清空30天前的过期日志
     */
    @Transactional
    public int clearExpireLog() {
        return taskExecuteLogDao.clearExpireLog();
    }

    // ==================== 扩展方法 ====================

    /**
     * 清理指定天数前的日志
     */
    @Transactional
    public int clearLogsOlderThanDays(int days) {
        return taskExecuteLogDao.clearLogsOlderThanDays(days);
    }

    /**
     * 查询最近N条日志
     */
    public List<TaskExecuteLog> findLatest(int limit) {
        return taskExecuteLogDao.findLatest(limit);
    }

    /**
     * 统计指定任务的日志总数
     */
    public long countByTaskId(Long taskId) {
        return taskExecuteLogDao.countByTaskId(taskId);
    }
}