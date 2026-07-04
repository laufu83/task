package cn.fred.task;

import cn.fred.dao.TaskExecuteLogDao;
import cn.fred.dao.TaskInfoDao;
import cn.fred.entity.TaskExecuteLog;
import cn.fred.entity.TaskInfo;
import cn.fred.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
@EnableScheduling
@Slf4j
public class HttpScheduleTask {

    @Autowired
    private TaskInfoDao taskInfoDao;

    @Autowired
    private TaskExecuteLogDao taskExecuteLogDao;

    @Value("${task.max-concurrent:5}")
    private int maxConcurrent;

    @Value("${task.http-timeout:30000}")
    private int httpTimeout;

    // 连续失败阈值：3次
    private static final int FAIL_THRESHOLD = 3;

    private final Semaphore semaphore;

    public HttpScheduleTask() {
        this.semaphore = new Semaphore(maxConcurrent);
    }

    // ==================== 任务扫描（异步多任务执行） ====================
    @Scheduled(cron = "${task.scan-cron:0 * * * * ?}")
    public void scanAndRunTask() {
        log.info("【定时任务】开始扫描可执行任务");
        try {
            LocalDateTime now = LocalDateTime.now();
            List<TaskInfo> taskList = taskInfoDao.findTasksToExecute(now);
            log.info("【定时任务】查询到待执行任务数量：{}", taskList.size());

            for (TaskInfo task : taskList) {
                CompletableFuture.runAsync(() -> {
                    boolean acquired;
                    try {
                        acquired = semaphore.tryAcquire(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("任务{}获取并发许可被中断", task.getId());
                        return;
                    }

                    if (!acquired) {
                        log.warn("任务ID:{} 获取执行许可超时，系统并发已满，本次放弃执行", task.getId());
                        return;
                    }
                    try {
                        log.info("【定时任务】开始执行任务：ID={}, 名称={}", task.getId(), task.getTaskName());
                        executeSingleTask(task);
                    } catch (Exception e) {
                        log.error("任务异步执行异常，任务ID：{}", task.getId(), e);
                    } finally {
                        semaphore.release();
                    }
                }).exceptionally(ex -> {
                    log.error("任务异步调度异常，任务ID:{}", task.getId(), ex);
                    return null;
                });
            }
        } catch (Exception e) {
            log.error("【定时任务】批量扫描任务发生异常", e);
        }
        log.info("【定时任务】本轮任务扫描提交异步队列完成");
    }

    // ==================== 单任务执行 ====================
    public void executeSingleTask(TaskInfo task) {
        long start = System.currentTimeMillis();
        TaskExecuteLog executeLog = buildExecuteLog(task);
        String response = null;
        Exception lastException = null;
        int maxRetries = task.getMaxRetries() != null ? task.getMaxRetries() : 3;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = calculateNextRunTime(task.getCronExpr(), now);
        // 标记是否本次触发连续失败自动禁用
        boolean autoDisable = false;
        log.info("【执行】开始执行任务，ID={}, 名称={}, URL={}",
                task.getId(), task.getTaskName(), task.getHttpUrl());
        try {
            // 更新状态为执行中
            taskInfoDao.updateTaskStatus(task.getId(), 3);
            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    if (attempt > 0) {
                        log.info("【重试】任务ID：{}，第{}次重试", task.getId(), attempt);
                        long backoff = (long) Math.pow(2, attempt);
                        Thread.sleep(backoff * 1000L);
                    }
                    int timeout = task.getTimeoutSeconds() != null ? task.getTimeoutSeconds() * 1000 : httpTimeout;
                    response = HttpClientUtil.doRequest(
                            task.getHttpUrl(),
                            task.getRequestMethod(),
                            task.getHeaderParams(),
                            task.getRequestBody(),
                            timeout
                    );
                    log.info("【执行】HTTP请求成功，任务ID={}", task.getId());
                    break;
                } catch (Exception e) {
                    lastException = e;
                    log.warn("任务执行失败，任务ID：{}，尝试次数：{}/{}", task.getId(), attempt + 1, maxRetries + 1);
                }
            }

            if (response != null) {
                // 执行成功：连续失败次数清零
                executeLog.setExecuteStatus(1);
                executeLog.setResponseData(truncate(response, 2000));
                taskInfoDao.clearFailCount(task.getId());
                log.info("【成功】任务执行成功，任务ID：{}，已清空连续失败计数", task.getId());
            } else {
                // 全部重试失败：连续失败次数+1
                executeLog.setExecuteStatus(0);
                executeLog.setErrorMsg(lastException != null ? truncate(lastException.getMessage(), 2000) : "未知错误");
                int currentFail = taskInfoDao.increaseFailCount(task.getId());
                log.error("【失败】任务执行最终失败，任务ID：{}，当前连续失败次数：{}", task.getId(), currentFail);

                // 连续失败达到阈值，标记任务为不可用（停用）
                if (currentFail >= FAIL_THRESHOLD) {
                    taskInfoDao.updateTaskStatus(task.getId(), 0);
                    autoDisable = true;
                    log.error("【告警】任务ID:{} 连续失败{}次，已自动置为不可用，停止调度", task.getId(), FAIL_THRESHOLD);
                }
            }

        } catch (Exception e) {
            executeLog.setExecuteStatus(0);
            executeLog.setErrorMsg(truncate(e.getMessage(), 2000));
            log.error("【异常】任务执行异常，任务ID：{}", task.getId(), e);
            int currentFail = taskInfoDao.increaseFailCount(task.getId());
            if (currentFail >= FAIL_THRESHOLD) {
                taskInfoDao.updateTaskStatus(task.getId(), 0);
                autoDisable = true;
                log.error("【告警】任务ID:{} 连续失败{}次，已自动置为不可用", task.getId(), FAIL_THRESHOLD);
            }
        } finally {
            // 没有触发自动禁用，才把执行中3重置为启用1
            if (!autoDisable) {
                taskInfoDao.updateTaskStatus(task.getId(), 1);
            }

            // 无论成败必须刷新下次执行时间
            if (nextRun != null) {
                taskInfoDao.updateNextRunTime(task.getId(), nextRun, now);
                log.info("【更新】任务ID={}，刷新下次执行时间:{}", task.getId(), nextRun);
            }

            long cost = System.currentTimeMillis() - start;
            executeLog.setCostTime(cost);
            executeLog.setExecuteTime(LocalDateTime.now());
            taskExecuteLogDao.saveLog(executeLog);
            log.info("【完成】任务执行完毕，任务ID：{}，耗时：{}ms", task.getId(), cost);
        }
    }

    // ==================== 日志清理 ====================
    @Scheduled(cron = "${task.log-clear-cron:0 0 2 * * ?}")
    public void clearExpireLog() {
        log.info("【日志清理】开始清理30天前的执行日志");
        try {
            int deleted = taskExecuteLogDao.clearExpireLog();
            log.info("【日志清理】完成，删除 {} 条记录", deleted);
        } catch (Exception e) {
            log.error("【日志清理】异常", e);
        }
    }

    // ==================== 工具方法 ====================
    private TaskExecuteLog buildExecuteLog(TaskInfo task) {
        TaskExecuteLog log = new TaskExecuteLog();
        log.setTaskId(task.getId());
        log.setTaskName(task.getTaskName());
        log.setRequestMethod(task.getRequestMethod());
        log.setRequestUrl(task.getHttpUrl());
        log.setRequestHeader(task.getHeaderParams());
        log.setRequestBody(task.getRequestBody());
        return log;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private LocalDateTime calculateNextRunTime(String cronExpr, LocalDateTime from) {
        try {
            if (cronExpr == null || cronExpr.isEmpty()) {
                return null;
            }
            CronExpression cron = CronExpression.parse(cronExpr);
            return cron.next(from);
        } catch (Exception e) {
            log.error("Cron表达式解析失败: {}", cronExpr, e);
            return null;
        }
    }

    // ==================== 手动触发接口 ====================
    public void executeNow(Long taskId) {
        TaskInfo task = taskInfoDao.getById(taskId);
        if (task == null) {
            log.warn("任务不存在，taskId：{}", taskId);
            return;
        }
        if (task.getTaskStatus() != 1) {
            log.warn("任务未启用，taskId：{}", taskId);
            return;
        }
        log.info("手动触发执行任务，taskId：{}", taskId);
        executeSingleTask(task);
    }
}