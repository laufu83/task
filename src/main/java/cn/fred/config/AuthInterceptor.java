package cn.fred.config;

import cn.fred.entity.SysUser;
import cn.fred.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Autowired
    private SysUserService userService;
    private final List<String> allowUrls = Arrays.asList("/init", "/login", "/doLogin", "/health");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        boolean pass = allowUrls.stream().anyMatch(uri::startsWith);
        if (pass) return true;
        if (userService.count() == 0) {
            response.sendRedirect("/init/form");
            return false;
        }
        SysUser loginUser = (SysUser) request.getSession().getAttribute("loginUser");
        if (loginUser == null) {
            response.sendRedirect("/login");
            return false;
        }
        return true;
    }
}