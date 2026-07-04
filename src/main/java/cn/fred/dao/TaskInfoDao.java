package cn.fred.dao;

import cn.fred.common.BaseDao;
import cn.fred.common.PageResult;
import cn.fred.entity.TaskInfo;
import org.springframework.stereotype.Repository;

@Repository
public class TaskInfoDao extends BaseDao<TaskInfo> {

    public TaskInfoDao() {
        super(TaskInfo.class);
    }
    public PageResult<TaskInfo> pageTask(long pageNum, long pageSize) {
        String countSql = "SELECT COUNT(*) FROM task_info";
        String listSql = "SELECT * FROM task_info ORDER BY create_time DESC LIMIT ?,?";
        return page(pageNum, pageSize, countSql, listSql);
    }
    public TaskInfo getById(Long id) {
        return findById("SELECT * FROM task_info WHERE id = ?", id);
    }
    public int insertTask(TaskInfo task) {
        String sql = "INSERT INTO task_info(task_name,cron_expr,http_url,request_method,request_body,header_params,task_status,remark) VALUES (?,?,?,?,?,?,?,?)";
        return insert(sql, task.getTaskName(), task.getCronExpr(), task.getHttpUrl(), task.getRequestMethod(), task.getRequestBody(), task.getHeaderParams(), task.getTaskStatus(), task.getRemark()).intValue();
    }
    public int updateTask(TaskInfo task) {
        String sql = "UPDATE task_info SET task_name=?,cron_expr=?,http_url=?,request_method=?,request_body=?,header_params=?,task_status=?,remark=?,last_execute_time=NOW() WHERE id=?";
        return update(sql, task.getTaskName(), task.getCronExpr(), task.getHttpUrl(), task.getRequestMethod(), task.getRequestBody(), task.getHeaderParams(), task.getTaskStatus(), task.getRemark(), task.getId());
    }
    public int deleteById(Long id) {
        return delete("DELETE FROM task_info WHERE id = ?", id);
    }
    public java.util.List<TaskInfo> listAllEnable() {
        return list("SELECT * FROM task_info WHERE task_status = 1");
    }
}