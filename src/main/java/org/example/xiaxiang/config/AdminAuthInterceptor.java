package org.example.xiaxiang.config;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.controller.AuthController;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;

/**
 * 后台访问鉴权拦截器
 *
 * 规则：
 * - 放行 /admin/login（登录页） 和 /admin/api/login（登录API）
 * - 其他 /admin/** 路径必须登录
 * - 未登录请求：
 *   · HTML 页面请求 → 自动跳 /admin/login?redirect=xxx
 *   · API 请求（/admin/api/*） → 返回 401 JSON
 * - 被踢下线（同账号在新设备登录）：
 *   · HTML 页面请求 → 跳 /admin/login?kicked=1&redirect=xxx
 *   · API 请求 → 返回 401 JSON，reason=kicked
 */
@Slf4j
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    /** 白名单：不需要登录 */
    private static final List<String> WHITELIST = Arrays.asList(
            "/admin/login",
            "/admin/api/login"
    );

    /** session 中标记"被踢下线"的属性名 */
    private static final String KICKED_ATTR = "KICKED_OUT";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && ctx.length() > 0 && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }

        // 白名单直接放行
        for (String wl : WHITELIST) {
            if (path.equals(wl)) {
                return true;
            }
        }

        HttpSession session = request.getSession(false);
        boolean logged = false;
        boolean kicked = false;
        if (session != null) {
            Object u = session.getAttribute(AuthController.SESSION_USER_KEY);
            logged = (u != null);
            if (!logged) {
                // 检查是否是被踢下线的 session
                Object k = session.getAttribute(KICKED_ATTR);
                kicked = (k != null);
            }
        }

        if (logged) {
            return true;
        }

        // 被踢下线：一次性提示，清除标记
        if (kicked) {
            try { session.removeAttribute(KICKED_ATTR); } catch (Exception ignore) {}
        }

        // API 请求返回 401 JSON
        if (path.startsWith("/admin/api/")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            String reason = kicked ? "kicked" : "unauthorized";
            String msg = kicked ? "您的账号在其他设备登录，您已被迫下线" : "未登录，请先登录";
            response.getWriter().write("{\"success\":false,\"message\":\"" + msg + "\",\"reason\":\"" + reason + "\"}");
            return false;
        }

        // 页面请求跳登录页
        String redirect = request.getRequestURI();
        if (request.getQueryString() != null) {
            redirect = redirect + "?" + request.getQueryString();
        }
        String kickedParam = kicked ? "kicked=1&" : "";
        response.sendRedirect(request.getContextPath() + "/admin/login?" + kickedParam + "redirect=" + java.net.URLEncoder.encode(redirect, "UTF-8"));
        return false;
    }
}
