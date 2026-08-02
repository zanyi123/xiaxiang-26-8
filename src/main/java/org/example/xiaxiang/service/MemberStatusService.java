package org.example.xiaxiang.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 成员权限状态管理服务
 *
 * 管理实践成员账号的启用/禁用状态：
 * - 内存中使用 ConcurrentHashMap 保证线程安全
 * - 持久化到 data/member-status.json，重启不丢失
 * - 只存储被禁用的用户名集合（默认全部启用）
 */
@Slf4j
@Service
public class MemberStatusService {

    private static final String DATA_DIR = "data";
    private static final String STATUS_FILE = "data/member-status.json";

    /** 被禁用的用户名集合 */
    private final Set<String> disabledUsers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @PostConstruct
    public void init() {
        loadFromFile();
    }

    /**
     * 判断用户是否启用
     */
    public boolean isEnabled(String username) {
        return !disabledUsers.contains(username);
    }

    /**
     * 启用用户
     */
    public void enable(String username) {
        disabledUsers.remove(username);
        saveToFile();
        log.info("[成员管理] 启用用户: {}", username);
    }

    /**
     * 禁用用户
     */
    public void disable(String username) {
        disabledUsers.add(username);
        saveToFile();
        log.info("[成员管理] 禁用用户: {}", username);
    }

    /**
     * 切换用户状态，返回切换后的状态（true=启用）
     */
    public boolean toggle(String username) {
        if (disabledUsers.contains(username)) {
            enable(username);
            return true;
        } else {
            disable(username);
            return false;
        }
    }

    /**
     * 获取所有被禁用的用户名
     */
    public Set<String> getDisabledUsers() {
        return new HashSet<>(disabledUsers);
    }

    // ==================== 持久化 ====================

    private void loadFromFile() {
        Path path = Paths.get(STATUS_FILE);
        if (!Files.exists(path)) {
            log.info("[成员管理] 状态文件不存在，所有用户默认启用");
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String content = sb.toString().trim();
            if (content.isEmpty()) return;
            // 简单JSON解析：["user1","user2"]
            content = content.replace("[", "").replace("]", "").replace("\"", "").trim();
            if (content.isEmpty()) return;
            String[] names = content.split(",");
            for (String name : names) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    disabledUsers.add(trimmed);
                }
            }
            log.info("[成员管理] 加载 {} 个禁用用户", disabledUsers.size());
        } catch (IOException e) {
            log.warn("[成员管理] 加载状态文件失败: {}", e.getMessage());
        }
    }

    private void saveToFile() {
        try {
            Path dir = Paths.get(DATA_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path path = Paths.get(STATUS_FILE);
            StringBuilder sb = new StringBuilder("[");
            int i = 0;
            for (String name : disabledUsers) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(name).append("\"");
                i++;
            }
            sb.append("]");
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                writer.write(sb.toString());
            }
        } catch (IOException e) {
            log.warn("[成员管理] 保存状态文件失败: {}", e.getMessage());
        }
    }
}
