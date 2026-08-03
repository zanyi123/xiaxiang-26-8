package org.example.xiaxiang.controller;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.ObjectMetadata;
import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.example.xiaxiang.properties.AppProperties;
import org.example.xiaxiang.properties.CosProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * COS 素材上传管理 Controller
 *
 * 提供 Web 界面 + REST API，支持：
 * 1. 拖拽上传文件到指定分类目录
 * 2. 批量上传
 * 3. 查看 COS 已有文件列表
 * 4. 删除文件
 *
 * 访问地址：http://localhost:8080/admin/upload
 */
@Slf4j
@Controller
@RequestMapping("/admin")
public class UploadController {

    @Autowired
    private COSClient cosClient;

    @Autowired
    private CosProperties cosProperties;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private org.example.xiaxiang.service.SlotService slotService;

    @Autowired
    private org.example.xiaxiang.service.CosService cosService;

    /** 允许上传的文件扩展名 */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp",
            ".mp3", ".wav", ".aac", ".m4a",
            ".mp4", ".mov", ".m4v",
            ".splat", ".ply", ".obj", ".glb", ".gltf", ".fbx"
    );

    /** 分类目录配置 */
    private static final List<Map<String, String>> CATEGORIES = Arrays.asList(
            cat("images", "图片", "封面、风景、档案照片、印章等"),
            cat("audio", "音频", "故事朗读、方言录音"),
            cat("videos", "视频", "4K建筑视频、宣传片"),
            cat("models", "3D模型", ".splat / .ply / .glb 模型文件"),
            cat("", "根目录", "建筑封面图（cover-*.jpg）")
    );

    private static Map<String, String> cat(String dir, String label, String desc) {
        Map<String, String> m = new HashMap<>();
        m.put("dir", dir);
        m.put("label", label);
        m.put("desc", desc);
        return m;
    }

    /**
     * 写操作环境校验：所有环境（dev/prod）均允许写入 COS 和 YAML
     * 本地开发和正式部署共用同一 COS 桶，绑定关系写入各自 application.yml
     * 素材实时更新通过 YamlUpdater + Thymeleaf 重渲染实现
     */
    private Result<?> ensureProdForWrites() {
        return null;
    }

    // ==================== 页面 ====================

    @GetMapping("/upload")
    public String uploadPage(Model model) {
        model.addAttribute("categories", CATEGORIES);
        model.addAttribute("bucketName", cosProperties.getBucketName());
        model.addAttribute("region", cosProperties.getRegion());
        model.addAttribute("mockMode", appProperties.isMockMode());
        model.addAttribute("env", appProperties.getEnv());
        model.addAttribute("isProd", appProperties.isProdEnv());
        return "admin/upload";
    }

    // ==================== REST API ====================

    /**
     * 上传单个文件
     * POST /admin/api/upload
     * 参数: file(文件), category(目录: images/audio/videos/models/"")
     * 参数: customName(可选，自定义文件名，不含路径)
     */
    @PostMapping("/api/upload")
    @ResponseBody
    public Result<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "images") String category,
            @RequestParam(value = "customName", required = false) String customName) {

        Result<?> guard = ensureProdForWrites();
        if (guard != null) return (Result<Map<String, Object>>) guard;

        if (file.isEmpty()) {
            return Result.fail("文件为空");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            return Result.fail("无法获取文件名");
        }

        // 校验扩展名
        String ext = getFileExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return Result.fail("不支持的文件类型: " + ext + "，允许: " + ALLOWED_EXTENSIONS);
        }

        // 生成 COS key
        String fileName = (customName != null && !customName.trim().isEmpty())
                ? customName.trim()
                : originalName;
        // 确保 customName 带正确扩展名
        if (!fileName.toLowerCase().endsWith(ext)) {
            fileName = fileName + ext;
        }

        String key = fileName;
        if (category != null && !category.trim().isEmpty()) {
            key = category.trim() + "/" + fileName;
        }

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            // 大文件用分片上传，小文件直接上传
            if (file.getSize() > 50 * 1024 * 1024) {
                // 分片上传（Spring MultipartFile 会暂存到临时文件）
                cosClient.putObject(
                        cosProperties.getBucketName(),
                        key,
                        file.getInputStream(),
                        metadata
                );
            } else {
                cosClient.putObject(
                        cosProperties.getBucketName(),
                        key,
                        file.getInputStream(),
                        metadata
                );
            }

            String url = String.format("https://%s.cos.%s.myqcloud.com/%s",
                    cosProperties.getBucketName(), cosProperties.getRegion(), key);

            log.info("[上传成功] {} → {} ({}KB)", originalName, key, file.getSize() / 1024);

            // 上传成功后清除文件存在性缓存，确保主页立即反映新素材
            cosService.clearCache();

            Map<String, Object> data = new HashMap<>();
            data.put("key", key);
            data.put("url", url);
            data.put("size", file.getSize());
            data.put("originalName", originalName);

            return Result.success(data, "上传成功");

        } catch (CosServiceException e) {
            log.error("[上传失败-COS服务异常] {}", e.getMessage());
            return Result.fail("COS服务异常: " + e.getMessage());
        } catch (CosClientException e) {
            log.error("[上传失败-COS客户端异常] {}", e.getMessage());
            return Result.fail("COS客户端异常: " + e.getMessage());
        } catch (Exception e) {
            log.error("[上传失败] {}", e.getMessage(), e);
            return Result.fail("上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传
     * POST /admin/api/upload-batch
     */
    @PostMapping("/api/upload-batch")
    @ResponseBody
    public Result<List<Map<String, Object>>> uploadBatch(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "category", defaultValue = "images") String category) {

        Result<?> guard = ensureProdForWrites();
        if (guard != null) return (Result<List<Map<String, Object>>>) guard;

        List<Map<String, Object>> results = new ArrayList<>();
        int success = 0;
        int fail = 0;

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                fail++;
                continue;
            }
            Result<Map<String, Object>> r = uploadFile(file, category, null);
            if (r.isSuccess()) {
                success++;
                results.add(r.getData());
            } else {
                fail++;
                Map<String, Object> err = new HashMap<>();
                err.put("originalName", file.getOriginalFilename());
                err.put("error", r.getMessage());
                results.add(err);
            }
        }

        String msg = String.format("批量上传完成: 成功 %d, 失败 %d", success, fail);
        return Result.success(results, msg);
    }

    /**
     * 列出 COS 指定目录下的文件
     * GET /admin/api/list?prefix=images/
     */
    @GetMapping("/api/list")
    @ResponseBody
    public Result<List<Map<String, Object>>> listFiles(
            @RequestParam(value = "prefix", defaultValue = "") String prefix) {

        try {
            List<com.qcloud.cos.model.COSObjectSummary> objects = cosClient.listObjects(
                    cosProperties.getBucketName(), prefix
            ).getObjectSummaries();

            List<Map<String, Object>> files = new ArrayList<>();
            for (com.qcloud.cos.model.COSObjectSummary obj : objects) {
                Map<String, Object> f = new HashMap<>();
                f.put("key", obj.getKey());
                f.put("size", obj.getSize());
                f.put("lastModified", obj.getLastModified().toString());
                f.put("url", String.format("https://%s.cos.%s.myqcloud.com/%s",
                        cosProperties.getBucketName(), cosProperties.getRegion(), obj.getKey()));
                files.add(f);
            }

            return Result.success(files, "共 " + files.size() + " 个文件");

        } catch (Exception e) {
            log.error("[列出文件失败] {}", e.getMessage());
            return Result.fail("列出文件失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     * DELETE /admin/api/delete?key=images/xxx.jpg
     */
    @DeleteMapping("/api/delete")
    @ResponseBody
    public Result<Void> deleteFile(@RequestParam("key") String key) {
        Result<?> guard = ensureProdForWrites();
        if (guard != null) return (Result<Void>) guard;

        try {
            cosClient.deleteObject(cosProperties.getBucketName(), key);
            log.info("[删除成功] {}", key);
            // 删除后清除缓存，确保主页立即反映删除
            cosService.clearCache();
            return Result.success(null, "删除成功: " + key);
        } catch (Exception e) {
            log.error("[删除失败] {}", e.getMessage());
            return Result.fail("删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取上传统计（各目录文件数）
     * GET /admin/api/stats
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        String[] prefixes = {"images/", "audio/", "videos/", "models/", ""};

        for (String prefix : prefixes) {
            try {
                List<com.qcloud.cos.model.COSObjectSummary> objects = cosClient.listObjects(
                        cosProperties.getBucketName(), prefix
                ).getObjectSummaries();

                // 过滤掉目录占位符
                long count = objects.stream()
                        .filter(o -> !o.getKey().endsWith("/"))
                        .count();

                long totalSize = objects.stream()
                        .filter(o -> !o.getKey().endsWith("/"))
                        .mapToLong(com.qcloud.cos.model.COSObjectSummary::getSize)
                        .sum();

                String label = prefix.isEmpty() ? "root" : prefix.replace("/", "");
                stats.put(label + "_count", count);
                stats.put(label + "_size", totalSize);

            } catch (Exception e) {
                String label = prefix.isEmpty() ? "root" : prefix.replace("/", "");
                stats.put(label + "_count", 0);
                stats.put(label + "_size", 0L);
            }
        }

        stats.put("mockMode", appProperties.isMockMode());
        stats.put("bucket", cosProperties.getBucketName());
        stats.put("region", cosProperties.getRegion());

        return Result.success(stats);
    }

    // ==================== 素材匹配中心 API ====================

    /**
     * GET /admin/api/slots
     * 返回所有 Slot（网站素材空位）信息
     */
    @GetMapping("/api/slots")
    @ResponseBody
    public Result<java.util.List<org.example.xiaxiang.service.SlotService.SlotInfo>> getSlots() {
        java.util.List<org.example.xiaxiang.service.SlotService.SlotInfo> slots = slotService.getAllSlots();
        return Result.success(slots, "共 " + slots.size() + " 个Slot");
    }

    /**
     * POST /admin/api/match
     * Body: { "matches": { "IMG-02-01": "images/diaolou.jpg", ... } }
     * 执行匹配：内存写入 AppProperties + 写盘 application.yml
     */
    @PostMapping("/api/match")
    @ResponseBody
    public Result<java.util.Map<String, Object>> applyMatches(
            @RequestBody java.util.Map<String, Object> body) {

        Result<?> guard = ensureProdForWrites();
        if (guard != null) return (Result<java.util.Map<String, Object>>) guard;

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> matches = (java.util.Map<String, String>) body.get("matches");
        if (matches == null || matches.isEmpty()) {
            return Result.fail("请先选择匹配项");
        }

        org.example.xiaxiang.service.SlotService.MatchResult r = slotService.applyMatches(matches);

        // 匹配写入YAML后清除缓存，确保主页重新判定填充状态
        cosService.clearCache();

        java.util.Map<String, Object> resp = new HashMap<>();
        resp.put("successCount", r.getSuccess().size());
        resp.put("failCount", r.getFail().size());
        resp.put("yamlWrittenCount", r.getYamlWrittenCount());
        resp.put("successIds", r.getSuccess());
        resp.put("failDetails", r.getFail());

        String msg = String.format("匹配完成：成功 %d 条，失败 %d 条",
                r.getSuccess().size(), r.getFail().size());
        if (r.getYamlWrittenCount() < r.getSuccess().size()) {
            msg += "（YAML 落盘 " + r.getYamlWrittenCount() + " 条）";
        }
        return Result.success(resp, msg);
    }

    /**
     * POST /admin/api/unmatch?slotId=IMG-01-01
     * 取消单个Slot的素材绑定（将YAML字段置空），用于错误绑定的解绑或重新绑定
     */
    @PostMapping("/api/unmatch")
    @ResponseBody
    public Result<java.util.Map<String, Object>> unmatchSlot(@RequestParam String slotId) {
        Result<?> guard = ensureProdForWrites();
        if (guard != null) return (Result<java.util.Map<String, Object>>) guard;

        org.example.xiaxiang.service.SlotService.MatchResult r = slotService.unbindSlot(slotId);
        java.util.Map<String, Object> resp = new HashMap<>();
        resp.put("successCount", r.getSuccess().size());
        resp.put("failCount", r.getFail().size());
        resp.put("yamlWrittenCount", r.getYamlWrittenCount());
        resp.put("successIds", r.getSuccess());
        resp.put("failDetails", r.getFail());

        if (r.getFail().isEmpty()) {
            return Result.success(resp, "已取消绑定：" + slotId);
        } else {
            return Result.fail("取消绑定失败：" + String.join(", ", r.getFail()));
        }
    }

    // ==================== 工具方法 ====================

    private String getFileExtension(String fileName) {
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx < 0) return "";
        return fileName.substring(dotIdx).toLowerCase();
    }
}
