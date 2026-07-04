package cn.fred.service;

import cn.fred.common.PageResult;
import cn.fred.entity.TaskInfo;
import cn.fred.dao.TaskInfoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TaskInfoService {
    @Autowired
    private TaskInfoDao taskInfoDao;

    public PageResult<TaskInfo> page(long pageNum, long pageSize) {
        return taskInfoDao.pageTask(pageNum, pageSize);
    }
    public TaskInfo getById(Long id) {
        return taskInfoDao.getById(id);
    }
    public void save(TaskInfo task) {
        taskInfoDao.insertTask(task);
    }
    public void update(TaskInfo task) {
        taskInfoDao.updateTask(task);
    }
    public void delete(Long id) {
        taskInfoDao.deleteById(id);
    }
    public List<TaskInfo> listEnableTask() {
        return taskInfoDao.listAllEnable();
    }
}