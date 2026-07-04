package cn.fred.controller;

import cn.fred.common.PageResult;
import cn.fred.entity.TaskInfo;
import cn.fred.service.TaskInfoService;
import cn.fred.task.HttpScheduleTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/task")
public class TaskController {
    @Autowired
    private TaskInfoService taskService;
    @Autowired
    private HttpScheduleTask httpScheduleTask;

    private static boolean isValid(String cron) {
        if (cron == null || cron.isBlank()) {
            return false;
        }
        try {
            CronExpression.parse(cron);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    @GetMapping("/list")
    public String list(Model model, @RequestParam(defaultValue = "1") long pageNum, @RequestParam(defaultValue = "10") long pageSize) {
        PageResult<TaskInfo> page = taskService.page(pageNum, pageSize);
        model.addAttribute("page", page);
        return "task/list";
    }
    @GetMapping("/execute")
    public String executeTask(@RequestParam Long id) {
        TaskInfo task = taskService.getById(id);
        if (task == null) {
            return "redirect:/task/list";
        }
        // 直接调用已有任务执行方法
        httpScheduleTask.executeSingleTask(task);
        // 执行完成跳转到该任务日志列表页
        return "redirect:/log/list?taskId=" + id;
    }
    @GetMapping("/add")
    public String add(Model model) {
        model.addAttribute("task", new TaskInfo());
        return "task/edit";
    }

    @GetMapping("/edit")
    public String edit(Model model, @RequestParam Long id) {
        TaskInfo task = taskService.getById(id);
        model.addAttribute("task", task);
        return "task/edit";
    }

    @PostMapping("/save")
    public String save(TaskInfo task, Model model) {
        if (!isValid(task.getCronExpr())) {
            model.addAttribute("task", task);
            model.addAttribute("errorMsg", "Cron表达式格式错误，请遵循：秒 分 时 日 月 星期 六段格式");
            return "task/edit";
        }
        if (task.getId() == null) {
            task.setTaskStatus(1);
            taskService.save(task);
        } else {
            taskService.update(task);
        }
        return "redirect:/task/list";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam Long id) {
        taskService.delete(id);
        return "redirect:/task/list";
    }

    @GetMapping("/log")
    public String toLog(@RequestParam Long taskId) {
        return "redirect:/log/list?taskId=" + taskId;
    }
}