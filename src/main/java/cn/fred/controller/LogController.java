package cn.fred.controller;


import cn.fred.common.PageResult;
import cn.fred.entity.TaskExecuteLog;
import cn.fred.service.TaskExecuteLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/log")
public class LogController {
    @Autowired
    private TaskExecuteLogService logService;

    @GetMapping("/list")
    public String list(Model model,
                       @RequestParam(defaultValue = "1") Long pageNum,
                       @RequestParam(defaultValue = "10") Long pageSize,
                       @RequestParam(required = false) Long taskId) {
        PageResult<TaskExecuteLog> page = logService.page(pageNum, pageSize, taskId);
        model.addAttribute("page", page);
        model.addAttribute("taskId", taskId);
        return "log/list";
    }

    @GetMapping("/detail")
    public String detail(Model model, @RequestParam Long id) {
        TaskExecuteLog log = logService.getById(id);
        model.addAttribute("log", log);
        return "log/detail";
    }
    @GetMapping("/clear")
    public String clearTaskLog(@RequestParam Long taskId){
        logService.clearTaskAllLog(taskId);
        return "redirect:/log/list?taskId=" + taskId;
    }
}