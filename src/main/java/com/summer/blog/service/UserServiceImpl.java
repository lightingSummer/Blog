package com.summer.blog.service;

import com.summer.blog.dao.TicketMapper;
import com.summer.blog.dao.UserMapper;
import com.summer.blog.model.Ticket;
import com.summer.blog.model.User;
import com.summer.blog.util.BlogUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author     ：summerGit
 * @date       ：2019/5/20 0020
 * @description：
 */
@Service
public class UserServiceImpl implements UserService {

    //用户名合法性正则：只能含有数字、汉字、字母、下划线
    private static final String USER_NAME_LEGAL_REGEX = "^[\\dA-Za-z_\\u4e00-\\u9fa5]+$";

    //用户密码合法性正则：只能含有数字、字母、下划线和特殊字符
    private static final String USER_PASSWORD_LEGAL_REGEX = "^[\\dA-Za-z_._~!@#$^&*]+$";

    //用户密码弱强度正则：纯数字或者纯字母
    private static final String USER_PASSWORD_STRONG_REGEX = "^(\\d*|[a-zA-Z]*)$";

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TicketMapper ticketMapper;

    @Override
    public User selectNameAndUrlById(int id) {
        return userMapper.selectNameAndUrlById(id);
    }

    /**
     * @author: lightingSummer
     * @date: 2019/5/21 0021
     * @description: 用户注册
     * @param name 用户名
     * @param password 用户密码
     * @return java.util.Map<java.lang.String, java.lang.Object> 错误信息
     */
    @Override
    public Map<String, Object> register(String name, String password) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isBlank(name)) {
            map.put("msgname", "用户名不能为空");
            return map;
        }
        if (name.length() > 10) {
            map.put("msgname", "用户名过长");
            return map;
        }
        //判断用户名是否包含非法字符
        Pattern namePattern = Pattern.compile(USER_NAME_LEGAL_REGEX);
        Matcher nameMatcher = namePattern.matcher(name);
        if (!nameMatcher.matches()) {
            map.put("msgname", "用户名存在非法字符");
            return map;
        }
        //密码长度检测
        if (StringUtils.isBlank(password)) {
            map.put("msgpwd", "用户密码不能为空");
            return map;
        } else if (password.length() < 6) {
            map.put("msgpwd", "用户密码过短");
            return map;
        } else if (password.length() > 20) {
            map.put("msgpwd", "用户密码过长");
            return map;
        }
        //判断密码是否包含非法字符
        Pattern psdPattern = Pattern.compile(USER_PASSWORD_LEGAL_REGEX);
        Matcher psdMatcher = psdPattern.matcher(password);
        if (!psdMatcher.matches()) {
            map.put("msgpwd", "用户密码存在非法字符");
            return map;
        }
        //密码强度检测
        Pattern psdStrongPattern = Pattern.compile(USER_PASSWORD_STRONG_REGEX);
        Matcher psdStrongMatcher = psdStrongPattern.matcher(password);
        if (psdStrongMatcher.matches()) {
            map.put("msgpwd", "用户密码弱爆了");
            return map;
        }

        User userIfEmpty = userMapper.selectByName(name);
        if (userIfEmpty != null) {
            map.put("msgname", "用户名已经被注册");
            return map;
        }

        //可以正常注册
        User user = new User();
        String salt = UUID.randomUUID().toString().substring(0, 5);
        String head = String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000));
        user.setName(name);
        user.setSalt(salt);
        user.setPassword(BlogUtil.MD5(password + salt));
        user.setHeadUrl(head);
        userMapper.insertSelective(user);
        //获取用户id
        User temp = userMapper.selectByName(name);
        String ticket = addUserLoginTicket(temp.getId());
        map.put("ticket", ticket);
        return map;
    }

    /**
     * @author: lightingSummer
     * @date: 2019/5/21 0021
     * @description: 登录
     * @param name
     * @param password
     * @return java.util.Map<java.lang.String, java.lang.Object>
     */
    @Override
    public Map<String, Object> login(String name, String password) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isBlank(name)) {
            map.put("msgname", "用户名不能为空");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("msgpwd", "用户密码不能为空");
            return map;
        }
        User userIfEmpty = userMapper.selectByName(name);
        if (userIfEmpty == null) {
            map.put("msgname", "用户名不存在");
            return map;
        }
        if (!userIfEmpty.getPassword().equals(BlogUtil.MD5(password + userIfEmpty.getSalt()))) {
            map.put("msgpwd", "密码不正确");
            return map;
        }
        //正常登录
        String ticket = addUserLoginTicket(userIfEmpty.getId());
        map.put("ticket", ticket);
        return map;
    }

    /**
     * @author: lightingSummer
     * @date: 2019/5/21 0021
     * @description: addUserLoginTicket
     * @param userId
     * @return java.lang.String ticket
     */
    @Override
    public String addUserLoginTicket(int userId) {
        Ticket ticket = new Ticket();
        ticket.setUserId(userId);
        String resTicket = UUID.randomUUID().toString().replaceAll("-", "");
        ticket.setTicket(resTicket);
        Date date = new Date();
        date.setTime(date.getTime() + 1000 * 3600 * 24);
        ticket.setExpired(date);
        ticket.setStatus(0);
        ticketMapper.insertSelective(ticket);
        return resTicket;
    }

    /**
     * @author: lightingSummer
     * @date: 2019/6/1 0001
     * @description: 退出
     */
    @Override
    public void logout(String ticket) {
        ticketMapper.setStatus(ticket, 1);
    }

    /**
     * @author: lightingSummer
     * @date: 2019/6/1 0001
     * @description: 通过name查询用户
     */
    @Override
    public int selectIdByName(String name) {
        return userMapper.selectByName(name).getId();
    }
}
