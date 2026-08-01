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
 */
@Slf4j
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    /** 白名单：不需要登录 */
    private static final List<String> WHITELIST = Arrays.asList(
            "/admin/login",
            "/admin/api/login"
    );

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
        if (session != null) {
            Object u = session.getAttribute(AuthController.SESSION_USER_KEY);
            logged = (u != null);
        }

        if (logged) {
            return true;
        }

        // 未登录：API 返回 401，页面跳登录
        if (path.startsWith("/admin/api/")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"未登录，请先登录\"}");
            return false;
        }

        // 页面请求跳登录，带上 redirect 参数
        String redirect = request.getRequestURI();
        if (request.getQueryString() != null) {
            redirect = redirect + "?" + request.getQueryString();
        }
        response.sendRedirect(request.getContextPath() + "/admin/login?redirect=" + java.net.URLEncoder.encode(redirect, "UTF-8"));
        return false;
    }
}
