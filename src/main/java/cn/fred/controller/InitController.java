package cn.fred.controller;


import cn.fred.entity.SysUser;
import cn.fred.service.SysUserService;
import cn.fred.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/init")
public class InitController {
    @Autowired
    private SysUserService userService;

    @GetMapping("/form")
    public String initForm() {
        if (userService.count() > 0) return "redirect:/login";
        return "init/admin_init";
    }

    @PostMapping("/save")
    public String saveAdmin(@RequestParam String username,
                            @RequestParam String password,
                            @RequestParam String confirmPwd,
                            Model model) {
        if (!password.equals(confirmPwd)) {
            model.addAttribute("msg", "两次密码不一致");
            return "init/admin_init";
        }
        if (username.length() < 3 || password.length() < 6) {
            model.addAttribute("msg", "用户名至少3位，密码至少6位");
            return "init/admin_init";
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode(password));
        user.setRealName("系统初始管理员");
        user.setStatus(1);
        userService.save(user);
        return "redirect:/login";
    }
}