package org.example.xiaxiang.config;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.service.SessionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Session 销毁监听器
 *
 * 当 session 过期或被 invalidate 时，清理 SessionRegistry 中的映射，
 * 避免 activeSessions 持有已失效的 session 引用导致内存泄漏。
 */
@Slf4j
@Component
public class SessionCleanupListener implements HttpSessionListener {

    @Autowired
    private SessionRegistry sessionRegistry;

    /**
     * 通过 session attribute 反查 username（登录时已存入 userInfo）
     */
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        try {
            Object userInfo = session.getAttribute("CURRENT_ADMIN_USER");
            if (userInfo instanceof java.util.Map) {
                Object username = ((java.util.Map<?, ?>) userInfo).get("username");
                if (username instanceof String) {
                    // 仅当当前注册的 session 就是这个被销毁的 session 时才清理
                    // （若是被新登录顶替的，register() 已替换为新 session，这里不会误删）
                    if (sessionRegistry.isCurrent((String) username, session)) {
                        sessionRegistry.unregister((String) username);
                        log.debug("[会话清理] 用户 {} 的 session 已销毁，清理映射", username);
                    }
                }
            }
        } catch (IllegalStateException e) {
            // session 已失效，getAttribute 抛异常是正常的，忽略
        }
    }
}
