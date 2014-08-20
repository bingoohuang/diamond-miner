package org.n3r.diamond.server.controller;

import org.n3r.diamond.server.service.AdminService;
import org.n3r.diamond.server.utils.DiamondServerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/login.do")
public class LoginController {
    @Autowired
    private AdminService adminService;


    @RequestMapping(params = "method=login", method = RequestMethod.POST)
    public String login(HttpServletRequest request, @RequestParam("username") String username,
                        @RequestParam("password") String password, ModelMap modelMap) {
        if (adminService.login(username, password)) {
            request.getSession().setAttribute("user", username);

            String result = DiamondServerUtils.processJson(request, modelMap, "login successfully");
            return result != null ? result : "admin/admin";
        } else {
            String result = DiamondServerUtils.processJson(request, modelMap, "login failed");
            if (result != null) return result;

            modelMap.addAttribute("message", "Login failed, username or password error");
            return "login";
        }
    }

    @RequestMapping(params = "method=logout", method = RequestMethod.GET)
    public String logout(HttpServletRequest request, ModelMap modelMap) {
        request.getSession().invalidate();

        String result = DiamondServerUtils.processJson(request, modelMap, "logout successfully");
        return result != null ? result : "login";
    }
}
