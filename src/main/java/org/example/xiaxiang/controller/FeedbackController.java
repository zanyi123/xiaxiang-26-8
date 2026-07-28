package org.example.xiaxiang.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final ConcurrentLinkedQueue<FeedbackItem> feedbackQueue = new ConcurrentLinkedQueue<>();

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

        log.info("[FeedbackController] 反馈已保存，ID={}", item.getId());

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
