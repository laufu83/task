package cn.fred.service;

import cn.fred.common.PageResult;
import cn.fred.entity.TaskInfo;
import cn.fred.dao.TaskInfoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskInfoService {

    @Autowired
    private TaskInfoDao taskInfoDao;

    // ==================== 基础 CRUD ====================

    public PageResult<TaskInfo> page(long pageNum, long pageSize) {
        return taskInfoDao.pageTask(pageNum, pageSize);
    }

    public TaskInfo getById(Long id) {
        return taskInfoDao.getById(id);
    }

    @Transactional
    public void save(TaskInfo task) {
        if (task.getTaskStatus() == null) {
            task.setTaskStatus(1);
        }
        if (task.getRequestMethod() == null || task.getRequestMethod().isEmpty()) {
            task.setRequestMethod("GET");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = calculateNextRunTime(task.getCronExpr(), now);
        task.setNextRunTime(nextRun);
        task.setLastExecuteTime(null);
        taskInfoDao.insertTask(task);
    }

    @Transactional
    public void update(TaskInfo task) {
        TaskInfo old = getById(task.getId());
        if (old != null && task.getCronExpr() != null
                && !old.getCronExpr().equals(task.getCronExpr())) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextRun = calculateNextRunTime(task.getCronExpr(), now);
            task.setNextRunTime(nextRun);
        }
        taskInfoDao.updateTask(task);
    }

    public void delete(Long id) {
        taskInfoDao.deleteById(id);
    }

    // ==================== 调度任务专用 ====================

    public List<TaskInfo> listEnableTask() {
        return taskInfoDao.listAllEnable();
    }

    public List<TaskInfo> findTasksToExecute(LocalDateTime now) {
        return taskInfoDao.findTasksToExecute(now);
    }

    @Transactional
    public boolean updateNextRunTime(Long taskId, String cronExpr, LocalDateTime lastExecuteTime) {
        LocalDateTime nextRun = calculateNextRunTime(cronExpr, lastExecuteTime);
        int rows = taskInfoDao.updateNextRunTime(taskId, nextRun, lastExecuteTime);
        return rows > 0;
    }

    @Transactional
    public boolean updateTaskStatus(Long taskId, Integer status) {
        int rows = taskInfoDao.updateTaskStatus(taskId, status);
        return rows > 0;
    }

    @Transactional
    public boolean incrementRetryCount(Long taskId) {
        int rows = taskInfoDao.incrementRetryCount(taskId);
        return rows > 0;
    }

    /**
     * 立即触发任务（将 next_run_time 设为当前时间）
     */
    @Transactional
    public boolean triggerNow(Long taskId) {
        LocalDateTime now = LocalDateTime.now();
        int rows = taskInfoDao.updateNextRunTime(taskId, now, null);
        return rows > 0;
    }

    /**
     * 重置任务（恢复启用，重置重试计数，重新计算下次执行时间）
     */
    @Transactional
    public boolean resetTask(Long taskId) {
        TaskInfo task = getById(taskId);
        if (task == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = calculateNextRunTime(task.getCronExpr(), now);
        int rows = taskInfoDao.updateNextRunTime(taskId, nextRun, null);
        if (rows > 0) {
            taskInfoDao.updateTaskStatus(taskId, 1);
            return true;
        }
        return false;
    }

    public LocalDateTime calculateNextRunTime(String cronExpr, LocalDateTime from) {
        try {
            if (cronExpr == null || cronExpr.isEmpty()) {
                return null;
            }
            CronExpression cron = CronExpression.parse(cronExpr);
            return cron.next(from);
        } catch (Exception e) {
            return null;
        }
    }
}