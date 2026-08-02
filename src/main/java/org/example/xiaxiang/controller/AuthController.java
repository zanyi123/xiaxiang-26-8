package org.example.xiaxiang.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.example.xiaxiang.properties.AdminProperties;
import org.example.xiaxiang.service.MemberStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台身份系统 Controller
 *
 * 提供：
 * 1. 登录页 /admin/login
 * 2. 登录API /admin/api/login (POST)
 * 3. 登出API /admin/api/logout (POST)
 * 4. 当前会话信息 /admin/api/me (GET)
 *
 * 会话存储：HttpSession（内存，重启失效，适合开发期/少量账号）
 */
@Slf4j
@Controller
@RequestMapping("/admin")
public class AuthController {

    @Autowired
    private AdminProperties adminProperties;

    @Autowired
    private MemberStatusService memberStatusService;

    public static final String SESSION_USER_KEY = "CURRENT_ADMIN_USER";

    // ==================== 登录页 ====================

    @GetMapping("")
    public String adminRoot() {
        return "redirect:/admin/login";
    }

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "redirect", required = false) String redirect,
            Model model) {
        // 已登录直接跳上传页
        return "admin/login";
    }

    // ==================== 登录/登出 API ====================

    @PostMapping("/api/login")
    @ResponseBody
    public Result<Map<String, Object>> login(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request) {

        AdminProperties.AdminUser user = adminProperties.getUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);

        if (user == null) {
            log.warn("[登录失败] 用户不存在: {}", username);
            return Result.fail("账号不存在");
        }

        if (!user.getPassword().equals(password)) {
            log.warn("[登录失败] 密码错误: {}", username);
            return Result.fail("密码错误");
        }

        // 检查账号是否被管理员禁用
        if (!memberStatusService.isEnabled(username)) {
            log.warn("[登录失败] 账号已被禁用: {}", username);
            return Result.fail("账号已被管理员禁用，请联系管理员");
        }

        // 创建会话
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(adminProperties.getSessionTimeoutMinutes() * 60);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", user.getUsername());
        userInfo.put("name", user.getName());
        userInfo.put("role", user.getRole());
        userInfo.put("description", user.getDescription());

        session.setAttribute(SESSION_USER_KEY, userInfo);

        log.info("[登录成功] {} ({})", user.getName(), user.getUsername());
        return Result.success(userInfo, "登录成功");
    }

    @PostMapping("/api/logout")
    @ResponseBody
    public Result<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Map<String, Object> u = (Map<String, Object>) session.getAttribute(SESSION_USER_KEY);
            if (u != null) {
                log.info("[登出] {}", u.get("name"));
            }
            session.invalidate();
        }
        return Result.success(null, "已登出");
    }

    @GetMapping("/api/me")
    @ResponseBody
    public Result<Map<String, Object>> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Result.fail("未登录");
        }
        Map<String, Object> u = (Map<String, Object>) session.getAttribute(SESSION_USER_KEY);
        if (u == null) {
            return Result.fail("未登录");
        }
        return Result.success(u);
    }

    /**
     * 列出所有账号（仅 ADMIN 可见，用于管理员查看有哪些实践同学）
     */
    @GetMapping("/api/users")
    @ResponseBody
    public Result<List<Map<String, Object>>> listUsers(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Result.fail("未登录");
        }
        Map<String, Object> cur = (Map<String, Object>) session.getAttribute(SESSION_USER_KEY);
        if (cur == null || !"ADMIN".equals(cur.get("role"))) {
            return Result.fail("仅管理员可见");
        }

        List<Map<String, Object>> list = adminProperties.getUsers().stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("username", u.getUsername());
                    m.put("name", u.getName());
                    m.put("role", u.getRole());
                    m.put("description", u.getDescription());
                    m.put("enabled", memberStatusService.isEnabled(u.getUsername()));
                    // 不返回密码
                    return m;
                })
                .collect(Collectors.toList());

        return Result.success(list);
    }

    // ==================== 成员权限管理 API（仅 ADMIN） ====================

    /**
     * 切换成员账号的启用/禁用状态
     */
    @PostMapping("/api/users/toggle")
    @ResponseBody
    public Result<Map<String, Object>> toggleUserStatus(
            @RequestParam String username,
            HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Result.fail("未登录");
        }
        Map<String, Object> cur = (Map<String, Object>) session.getAttribute(SESSION_USER_KEY);
        if (cur == null || !"ADMIN".equals(cur.get("role"))) {
            return Result.fail("仅管理员可操作");
        }

        // 不能禁用自己
        if (cur.get("username").equals(username)) {
            return Result.fail("不能禁用自己的账号");
        }

        // 查找用户是否存在
        AdminProperties.AdminUser target = adminProperties.getUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst().orElse(null);
        if (target == null) {
            return Result.fail("用户不存在: " + username);
        }

        // 不能禁用管理员
        if ("ADMIN".equals(target.getRole())) {
            return Result.fail("不能禁用管理员账号");
        }

        boolean nowEnabled = memberStatusService.toggle(username);
        log.info("[成员管理] {} {} 了用户 {}", cur.get("name"), nowEnabled ? "启用" : "禁用", target.getName());

        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("name", target.getName());
        result.put("enabled", nowEnabled);
        return Result.success(result, nowEnabled ? "已启用" : "已禁用");
    }
}
