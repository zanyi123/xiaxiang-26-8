package org.example.xiaxiang.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 反馈持久化文件路径，默认位于工作目录下的 data/feedback.jsonl
     * 每行一条 JSON 记录，便于追加写入与读取。
     */
    @Value("${app.feedback.file:data/feedback.jsonl}")
    private String feedbackFile;

    private final ConcurrentLinkedQueue<FeedbackItem> feedbackQueue = new ConcurrentLinkedQueue<>();

    /**
     * 启动时加载已有反馈记录到内存
     */
    @PostConstruct
    public void loadExistingFeedback() {
        File file = getFeedbackFile();
        if (file == null || !file.exists()) {
            log.info("[FeedbackController] 反馈文件不存在，跳过加载");
            return;
        }
        int count = 0;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    FeedbackItem item = objectMapper.readValue(line, FeedbackItem.class);
                    if (item != null) {
                        feedbackQueue.add(item);
                        count++;
                    }
                } catch (Exception e) {
                    log.warn("[FeedbackController] 解析反馈行失败，跳过：{}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[FeedbackController] 加载反馈文件失败", e);
        }
        log.info("[FeedbackController] 已加载 {} 条历史反馈", count);
    }

    @PostMapping
    public Result<Map<String, Object>> submitFeedback(@RequestBody FeedbackRequest request) {
        log.info("[FeedbackController] 收到反馈，type={}, name={}, contact={}",
                request.getType(), request.getName(), request.getContact());

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return Result.fail("反馈内容不能为空");
        }

        FeedbackItem item = new FeedbackItem();
        item.setId(System.currentTimeMillis());
        item.setType(request.getType() != null ? request.getType() : "general");
        item.setName(request.getName());
        item.setContact(request.getContact());
        item.setContent(request.getContent());
        item.setCreatedAt(java.time.LocalDateTime.now().toString());

        feedbackQueue.add(item);

        // 持久化到文件
        boolean saved = persistToFile(item);
        if (saved) {
            log.info("[FeedbackController] 反馈已保存，ID={}", item.getId());
        } else {
            log.warn("[FeedbackController] 反馈已存内存但写盘失败，ID={}", item.getId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", item.getId());
        result.put("message", "感谢您的反馈，我们会认真处理！");
        result.put("type", item.getType());

        return Result.success(result);
    }

    @GetMapping("/types")
    public Result<List<Map<String, String>>> getFeedbackTypes() {
        log.info("[FeedbackController] 获取反馈类型列表");

        List<Map<String, String>> types = new ArrayList<>();

        Map<String, String> t1 = new HashMap<>();
        t1.put("value", "general");
        t1.put("label", "一般建议");
        types.add(t1);

        Map<String, String> t2 = new HashMap<>();
        t2.put("value", "bug");
        t2.put("label", "问题反馈");
        types.add(t2);

        Map<String, String> t3 = new HashMap<>();
        t3.put("value", "content");
        t3.put("label", "内容纠错");
        types.add(t3);

        Map<String, String> t4 = new HashMap<>();
        t4.put("value", "cooperation");
        t4.put("label", "合作洽谈");
        types.add(t4);

        return Result.success(types);
    }

    /**
     * 获取全部反馈列表（供后台管理查看）
     */
    @GetMapping("/list")
    public Result<List<FeedbackItem>> listFeedback() {
        log.info("[FeedbackController] 获取反馈列表，共 {} 条", feedbackQueue.size());
        return Result.success(new ArrayList<>(feedbackQueue));
    }

    /**
     * 将单条反馈追加写入文件（JSON Lines 格式）
     */
    private synchronized boolean persistToFile(FeedbackItem item) {
        File file = ensureFeedbackFile();
        if (file == null) {
            return false;
        }
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new java.io.FileOutputStream(file, true), StandardCharsets.UTF_8))) {
            String json = objectMapper.writeValueAsString(item);
            pw.println(json);
            return true;
        } catch (Exception e) {
            log.error("[FeedbackController] 写入反馈文件失败", e);
            return false;
        }
    }

    /**
     * 定位反馈文件，若所在目录不存在则创建
     */
    private File ensureFeedbackFile() {
        try {
            File file = new File(feedbackFile);
            if (!file.isAbsolute()) {
                file = Paths.get(System.getProperty("user.dir"), feedbackFile).toFile();
            }
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs()) {
                    log.warn("[FeedbackController] 创建反馈目录失败：{}", parent.getAbsolutePath());
                    return null;
                }
            }
            return file;
        } catch (Exception e) {
            log.error("[FeedbackController] 定位反馈文件失败", e);
            return null;
        }
    }

    private File getFeedbackFile() {
        return ensureFeedbackFile();
    }

    @Data
    public static class FeedbackRequest {
        private String type;
        private String name;
        private String contact;
        private String content;
    }

    @Data
    public static class FeedbackItem {
        private Long id;
        private String type;
        private String name;
        private String contact;
        private String content;
        private String createdAt;
    }
}
