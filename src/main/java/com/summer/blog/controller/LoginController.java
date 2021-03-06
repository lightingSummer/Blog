package com.summer.blog.controller;

import com.summer.blog.async.EventModel;
import com.summer.blog.async.EventProducer;
import com.summer.blog.async.EventType;
import com.summer.blog.service.UserService;
import com.summer.blog.util.BlogUtil;
import com.summer.blog.util.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author     ：summerGit
 * @date       ：2019/5/21 0021
 * @description：
 */
@Controller
public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    @RequestMapping(value = "/reg/", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public String register(Model model, @RequestParam("username") String name,
                           @RequestParam("password") String password,
                           @RequestParam(value = "rember", defaultValue = "0") int remember,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        try {
            Map<String, Object> resp = userService.register(name, password);
            if (resp.containsKey("ticket")) {
                Cookie cookie = new Cookie("ticket", resp.get("ticket").toString());
                //全站有效
                cookie.setPath("/");
                if (remember > 0) {
                    cookie.setMaxAge(3600 * 24 * 7);
                }
                response.addCookie(cookie);

                //异步处理
                EventModel event = new EventModel();
                event.setType(EventType.LOGIN);
                event.setEntityOwnerId(userService.selectIdByName(name));
                event.setExts("ip", IpUtil.getIp(request));
                eventProducer.addEvent(event);

                return BlogUtil.getJSONString(0, "注册成功");
            } else {
                return BlogUtil.getJSONString(1, resp);
            }
        } catch (Exception e) {
            logger.error("注册异常" + e.getMessage());
            return BlogUtil.getJSONString(1, "注册异常");
        }
    }

    @RequestMapping(value = "/login/", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public String login(Model model, @RequestParam("username") String name,
                        @RequestParam("password") String password,
                        @RequestParam(value = "rember", defaultValue = "0") int remember,
                        HttpServletRequest request,
                        HttpServletResponse response) {
        try {
            Map<String, Object> resp = userService.login(name, password);
            if (resp.containsKey("ticket")) {
                Cookie cookie = new Cookie("ticket", resp.get("ticket").toString());
                //全站有效
                cookie.setPath("/");
                if (remember > 0) {
                    cookie.setMaxAge(3600 * 24 * 7);
                }
                response.addCookie(cookie);
                //异步处理
                EventModel event = new EventModel();
                event.setType(EventType.LOGIN);
                event.setEntityOwnerId(userService.selectIdByName(name));
                event.setExts("ip", IpUtil.getIp(request));
                eventProducer.addEvent(event);

                return BlogUtil.getJSONString(0, "登录成功");
            } else {
                return BlogUtil.getJSONString(1, resp);
            }
        } catch (Exception e) {
            logger.error("登录异常" + e.getMessage());
            return BlogUtil.getJSONString(1, "登录异常");
        }
    }

    @RequestMapping(value = "/logout/", method = {RequestMethod.GET, RequestMethod.POST})
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        return "redirect:/";
    }
}
