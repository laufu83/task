package cn.fred.service;



import cn.fred.common.PageResult;
import cn.fred.dao.TaskExecuteLogDao;
import cn.fred.entity.TaskExecuteLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskExecuteLogService {
    @Autowired
    private TaskExecuteLogDao logDao;
    public PageResult<TaskExecuteLog> page(Long pageNum, Long pageSize, Long taskId) {
        return logDao.pageLog(pageNum, pageSize, taskId);
    }
    public TaskExecuteLog getById(Long id) {
        return logDao.getById(id);
    }
    public void save(TaskExecuteLog log) {
        logDao.saveLog(log);
    }
    public void clear30DayLog() {
        logDao.clearExpireLog();
    }
    public void clearTaskAllLog(Long taskId) {
        logDao.clearByTaskId(taskId);
    }
}