package cn.fred.controller;

import cn.fred.common.PageResult;
import cn.fred.entity.TaskExecuteLog;
import cn.fred.service.TaskExecuteLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/log")
public class LogController {

    @Autowired
    private TaskExecuteLogService logService;

    /**
     * 执行日志列表
     */
    @GetMapping("/list")
    public String list(Model model,
                       @RequestParam(defaultValue = "1") Long pageNum,
                       @RequestParam(defaultValue = "10") Long pageSize,
                       @RequestParam(required = false) Long taskId) {
        PageResult<TaskExecuteLog> page = logService.page(pageNum, pageSize, taskId);
        model.addAttribute("page", page);
        model.addAttribute("taskId", taskId);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        return "log/list";
    }

    /**
     * 日志详情
     */
    @GetMapping("/detail")
    public String detail(Model model, @RequestParam Long id) {
        TaskExecuteLog log = logService.getById(id);
        if (log == null) {
            return "redirect:/log/list";
        }

        // ===== 添加 JSON 格式标记（在 Controller 中判断） =====
        model.addAttribute("log", log);
        model.addAttribute("headerIsJson", isJsonFormat(log.getRequestHeader()));
        model.addAttribute("bodyIsJson", isJsonFormat(log.getRequestBody()));
        model.addAttribute("responseIsJson", isJsonFormat(log.getResponseData()));

        return "log/detail";
    }

    /**
     * 判断字符串是否为 JSON 格式（以 { 或 [ 开头）
     */
    private boolean isJsonFormat(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String trimmed = text.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    /**
     * 清空指定任务的所有执行日志（POST方式更安全）
     */
    @PostMapping("/clear")
    public String clearTaskLog(@RequestParam Long taskId, RedirectAttributes redirectAttributes) {
        try {
            int deleted = logService.clearByTaskId(taskId);
            redirectAttributes.addFlashAttribute("success", "成功清空 " + deleted + " 条日志");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "清空日志失败：" + e.getMessage());
        }
        return "redirect:/log/list?taskId=" + taskId;
    }

    /**
     * 清空所有30天前的过期日志（系统任务，也可手动触发）
     */
    @PostMapping("/clear-expire")
    public String clearExpireLog(RedirectAttributes redirectAttributes) {
        try {
            int deleted = logService.clearExpireLog();
            redirectAttributes.addFlashAttribute("success", "成功清理 " + deleted + " 条过期日志（30天前）");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "清理过期日志失败：" + e.getMessage());
        }
        return "redirect:/log/list";
    }

    /**
     * 删除单条日志
     */
    @PostMapping("/delete")
    public String delete(@RequestParam Long id, @RequestParam(required = false) Long taskId,
                         RedirectAttributes redirectAttributes) {
        try {
            logService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "删除成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "删除失败：" + e.getMessage());
        }
        String redirectUrl = taskId != null ? "redirect:/log/list?taskId=" + taskId : "redirect:/log/list";
        return redirectUrl;
    }
}