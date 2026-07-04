package cn.fred.task;


import cn.fred.entity.TaskExecuteLog;
import cn.fred.entity.TaskInfo;
import cn.fred.service.TaskExecuteLogService;
import cn.fred.service.TaskInfoService;
import cn.fred.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@EnableScheduling
@Slf4j
public class HttpScheduleTask {
    @Autowired
    private TaskInfoService taskInfoService;
    @Autowired
    private TaskExecuteLogService logService;
    private final AtomicBoolean runLock = new AtomicBoolean(false);

    @Scheduled(cron = "0 * * * * ?")
    public void scanAndRunTask() {
        log.info("【定时任务】开始扫描可执行任务，当前时间：{}", LocalDateTime.now());
        if (!runLock.compareAndSet(false, true)) {
            log.warn("【定时任务】上一轮任务尚未执行完毕，本次跳过执行");
            return;
        }
        try {
            List<TaskInfo> taskList = taskInfoService.listEnableTask();
            log.info("【定时任务】查询到启用状态任务数量：{}", taskList.size());
            for (TaskInfo task : taskList) {
                log.info("【定时任务】准备执行任务：任务ID={}，任务名称={}，请求地址={}",
                        task.getId(), task.getTaskName(), task.getHttpUrl());
                executeSingleTask(task);
            }
        } catch (Exception e) {
            log.error("【定时任务】批量执行任务发生异常", e);
        } finally {
            runLock.set(false);
            log.info("【定时任务】本轮任务扫描执行结束");
        }
    }

    public void executeSingleTask(TaskInfo task) {
        long start = System.currentTimeMillis();
        TaskExecuteLog taskExecuteLog = new TaskExecuteLog();
        taskExecuteLog.setTaskId(task.getId());
        taskExecuteLog.setTaskName(task.getTaskName());
        taskExecuteLog.setRequestMethod(task.getRequestMethod());
        taskExecuteLog.setRequestUrl(task.getHttpUrl());
        taskExecuteLog.setRequestHeader(task.getHeaderParams());
        taskExecuteLog.setRequestBody(task.getRequestBody());
        try {
            log.info("【单次任务】开始请求：{} {}", task.getRequestMethod(), task.getHttpUrl());
            String resp = HttpClientUtil.doRequest(task.getHttpUrl(), task.getRequestMethod(), task.getHeaderParams(), task.getRequestBody());
            taskExecuteLog.setExecuteStatus(1);
            taskExecuteLog.setResponseData(resp.length() > 2000 ? resp.substring(0, 2000) : resp);
            log.info("【单次任务】任务执行成功，响应长度：{}", resp.length());
        } catch (Exception e) {
            taskExecuteLog.setExecuteStatus(0);
            taskExecuteLog.setErrorMsg(e.getMessage() != null && e.getMessage().length() > 2000 ? e.getMessage().substring(0, 2000) : e.getMessage());
            log.error("【单次任务】任务执行失败，任务ID：{}，异常信息：{}", task.getId(), e.getMessage(), e);
        }
        long cost = System.currentTimeMillis() - start;
        taskExecuteLog.setCostTime(cost);
        logService.save(taskExecuteLog);
        log.info("【单次任务】任务执行完毕，耗时：{}ms", cost);
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void clearExpireLog() {
        log.info("【日志清理任务】开始清理30天前的执行日志");
        try {
            logService.clear30DayLog();
            log.info("【日志清理任务】日志清理执行完成");
        } catch (Exception e) {
            log.error("【日志清理任务】清理日志异常", e);
        }
    }
}