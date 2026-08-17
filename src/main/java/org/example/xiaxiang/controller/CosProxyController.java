package org.example.xiaxiang.controller;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.properties.CosProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * COS 文件代理控制器
 * 绕过 COS 桶防盗链限制，通过后端代理转发文件
 */
@Slf4j
@RestController
@RequestMapping("/cos")
public class CosProxyController {

    @Autowired
    private COSClient cosClient;

    @Autowired
    private CosProperties cosProperties;

    /**
     * 代理 COS 文件请求
     * 支持 /cos/images/xxx.png, /cos/videos/xxx.mp4 等路径
     */
    @GetMapping("/{*key}")
    public void proxyFile(@PathVariable("key") String cosKey,
                          HttpServletRequest request,
                          HttpServletResponse response) {
        // 防止路径遍历
        if (cosKey == null || cosKey.isEmpty() || cosKey.contains("..")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            String bucketName = cosProperties.getBucketName();
            log.debug("[CosProxy] 代理请求：key={}", cosKey);

            COSObject cosObject = cosClient.getObject(bucketName, cosKey);
            COSObjectInputStream cosInput = cosObject.getObjectContent();

            // 设置响应头
            String contentType = cosObject.getObjectMetadata().getContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = guessContentType(cosKey);
            }
            response.setContentType(contentType);
            response.setContentLengthLong(cosObject.getObjectMetadata().getContentLength());

            // 添加缓存头
            response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=3600");

            // 流式复制
            try (InputStream is = cosInput; OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
        } catch (Exception e) {
            log.warn("[CosProxy] 代理请求失败：key={}, error={}", cosKey, e.getMessage());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * 根据文件扩展名猜测 ContentType
     */
    private String guessContentType(String key) {
        String lower = key.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".splat")) return "application/octet-stream";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".html")) return "text/html;charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        return "application/octet-stream";
    }
}
