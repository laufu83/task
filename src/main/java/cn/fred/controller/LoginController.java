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
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping
public class LoginController {
    @Autowired
    private SysUserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/doLogin")
    public String login(String username, String password, HttpSession session, Model model) {
        SysUser user = userService.getByUsername(username);
        if (user == null || !PasswordUtil.match(password, user.getPassword())) {
            model.addAttribute("msg", "账号或密码错误");
            return "login";
        }
        session.setAttribute("loginUser", user);
        return "redirect:/task/list";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/task/list";
    }
}