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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/task")
public class TaskController {

    @Autowired
    private TaskInfoService taskService;

    @Autowired
    private HttpScheduleTask httpScheduleTask;

    /**
     * 验证Cron表达式是否合法
     */
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

    /**
     * 任务列表
     */
    @GetMapping("/list")
    public String list(Model model,
                       @RequestParam(defaultValue = "1") long pageNum,
                       @RequestParam(defaultValue = "10") long pageSize) {
        PageResult<TaskInfo> page = taskService.page(pageNum, pageSize);
        model.addAttribute("page", page);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        return "task/list";
    }

    /**
     * 新增任务页面
     */
    @GetMapping("/add")
    public String add(Model model) {
        model.addAttribute("task", new TaskInfo());
        model.addAttribute("title", "新增任务");
        return "task/edit";
    }

    /**
     * 编辑任务页面
     */
    @GetMapping("/edit")
    public String edit(Model model, @RequestParam Long id) {
        TaskInfo task = taskService.getById(id);
        if (task == null) {
            return "redirect:/task/list";
        }
        model.addAttribute("task", task);
        model.addAttribute("title", "编辑任务");
        return "task/edit";
    }

    /**
     * 保存任务（新增或更新）
     */
    @PostMapping("/save")
    public String save(TaskInfo task, Model model, RedirectAttributes redirectAttributes) {
        // 验证Cron表达式
        if (!isValid(task.getCronExpr())) {
            model.addAttribute("task", task);
            model.addAttribute("errorMsg", "Cron表达式格式错误，请遵循：秒 分 时 日 月 星期 六段格式");
            model.addAttribute("title", task.getId() == null ? "新增任务" : "编辑任务");
            return "task/edit";
        }

        try {
            if (task.getId() == null) {
                task.setTaskStatus(1);
                taskService.save(task);
                redirectAttributes.addFlashAttribute("success", "任务创建成功");
            } else {
                taskService.update(task);
                redirectAttributes.addFlashAttribute("success", "任务更新成功");
            }
        } catch (Exception e) {
            model.addAttribute("task", task);
            model.addAttribute("errorMsg", "保存失败：" + e.getMessage());
            model.addAttribute("title", task.getId() == null ? "新增任务" : "编辑任务");
            return "task/edit";
        }
        return "redirect:/task/list";
    }

    /**
     * 删除任务
     */
    @PostMapping("/delete")
    public String delete(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            taskService.delete(id);
            redirectAttributes.addFlashAttribute("success", "任务删除成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "删除失败：" + e.getMessage());
        }
        return "redirect:/task/list";
    }

    /**
     * 立即执行任务（手动触发）
     */
    @GetMapping("/execute")
    public String executeTask(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        TaskInfo task = taskService.getById(id);
        if (task == null) {
            redirectAttributes.addFlashAttribute("error", "任务不存在");
            return "redirect:/task/list";
        }
        if (task.getTaskStatus() != 1) {
            redirectAttributes.addFlashAttribute("error", "任务未启用，无法执行");
            return "redirect:/task/list";
        }
        try {
            // 将任务的 next_run_time 设置为当前时间，让调度器马上扫描到
            taskService.triggerNow(id);
            redirectAttributes.addFlashAttribute("success", "任务已触发，请稍后查看执行日志");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "触发失败：" + e.getMessage());
        }
        return "redirect:/task/list";
    }

    /**
     * 切换任务状态（启用/停用）
     */
    @GetMapping("/toggle")
    public String toggleStatus(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        TaskInfo task = taskService.getById(id);
        if (task == null) {
            redirectAttributes.addFlashAttribute("error", "任务不存在");
            return "redirect:/task/list";
        }
        try {
            int newStatus = task.getTaskStatus() == 1 ? 0 : 1;
            taskService.updateTaskStatus(id, newStatus);
            String statusText = newStatus == 1 ? "启用" : "停用";
            redirectAttributes.addFlashAttribute("success", "任务已" + statusText);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "切换状态失败：" + e.getMessage());
        }
        return "redirect:/task/list";
    }

    /**
     * 重置任务（失败后恢复启用，重置重试计数）
     */
    @GetMapping("/reset")
    public String resetTask(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            boolean result = taskService.resetTask(id);
            if (result) {
                redirectAttributes.addFlashAttribute("success", "任务已重置，下次执行时间已重新计算");
            } else {
                redirectAttributes.addFlashAttribute("error", "重置失败，任务不存在");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "重置失败：" + e.getMessage());
        }
        return "redirect:/task/list";
    }

    /**
     * 查看任务执行日志
     */
    @GetMapping("/log")
    public String toLog(@RequestParam Long taskId) {
        return "redirect:/log/list?taskId=" + taskId;
    }
}