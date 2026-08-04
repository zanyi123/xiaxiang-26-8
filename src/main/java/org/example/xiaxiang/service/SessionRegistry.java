package org.example.xiaxiang.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 账号会话注册表 —— 实现"同账号单会话"互踢机制
 *
 * 规则：
 * - 同一个 username 全局只允许绑定一个活跃 HttpSession
 * - 新登录时，若该 username 已有旧会话，则旧会话被标记为"被踢下线"
 *   （不立即 invalidate，而是移除用户信息 + 打标记，让用户下次操作时收到提示）
 * - 多个不同 username 的会话互不影响，支持多人同时在线
 *
 * 适用场景：无数据库、单实例部署、实践成员数量少
 */
@Slf4j
@Service
public class SessionRegistry {

    /** username → 当前活跃的 HttpSession */
    private final ConcurrentHashMap<String, HttpSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * 注册新会话；若该用户已有旧会话，踢掉旧会话
     *
     * @param username 用户名
     * @param newSession 新登录创建的会话
     */
    public void register(String username, HttpSession newSession) {
        HttpSession oldSession = activeSessions.put(username, newSession);
        if (oldSession != null && oldSession != newSession) {
            try {
                // 不直接 invalidate（会让用户当前页面立即报错），
                // 而是移除登录态 + 打标记，用户下次操作时由拦截器引导到登录页并提示
                oldSession.removeAttribute("CURRENT_ADMIN_USER");
                oldSession.setAttribute("KICKED_OUT", Boolean.TRUE);
                log.info("[会话互踢] 用户 {} 的旧会话已被新登录顶替", username);
            } catch (IllegalStateException e) {
                // 旧会话已失效（过期或手动 invalidate），忽略
                log.debug("[会话互踢] 用户 {} 的旧会话已失效，无需处理", username);
            }
        }
    }

    /**
     * 注销时清理映射（主动登出）
     */
    public void unregister(String username) {
        activeSessions.remove(username);
    }

    /**
     * 判断当前 session 是否仍是该用户的活跃会话
     * （用于拦截器区分"被踢"与"正常未登录"）
     */
    public boolean isCurrent(String username, HttpSession session) {
        HttpSession current = activeSessions.get(username);
        return current != null && current == session;
    }
}
