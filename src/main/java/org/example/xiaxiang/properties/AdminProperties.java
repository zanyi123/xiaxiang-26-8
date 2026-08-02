package org.example.xiaxiang.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 后台身份系统配置映射
 * 与 application.yml 中的 admin.* 前缀配置绑定
 *
 * 无数据库账号体系：
 * - 全部账号配置在 YAML 里，随项目一起发布
 * - 会话使用 HttpSession（内存存储，重启失效）
 * - 增删账号直接改 YAML 重启即可，实践同学数量少时足够用
 */
@Data
@Component
@ConfigurationProperties(prefix = "admin")
public class AdminProperties {

    /** 会话超时（分钟），默认 8 小时 */
    private int sessionTimeoutMinutes = 480;

    /** 管理员/实践同学账号列表 */
    private List<AdminUser> users;

    @Data
    public static class AdminUser {
        private String username;
        private String password;
        private String role;        // ADMIN / VOLUNTEER
        private String name;
        private String description;
        private boolean enabled = true;  // 账号是否激活，默认true
    }
}
