package org.example.xiaxiang.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.properties.CosProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

/**
 * 腾讯云 COS 客户端配置类
 *
 * 防坑设计：
 * 1. COSClient 是线程安全的重量级对象，必须作为 Spring Bean 单例注入
 * 2. 在 @PreDestroy 中调用 shutdown() 释放连接池，防止内存泄漏
 * 3. secret-id / secret-key 绝不硬编码，全部来自 CosProperties
 */
@Slf4j
@Configuration
public class CosConfig {

    private COSClient cosClient;

    @Autowired
    private CosProperties cosProperties;

    /**
     * 注册 COSClient 单例 Bean
     * 启动时强制校验密钥，缺失则立即 FAIL FAST 并打印错误信息（避免yml被忽略默默崩溃）
     */
    @Bean
    public COSClient cosClient() {
        log.info("========== [配置检查] ==========");
        log.info("[配置] 正在初始化 COSClient...");

        String secretId = cosProperties.getSecretId();
        String secretKey = cosProperties.getSecretKey();
        String region = cosProperties.getRegion();
        String bucket = cosProperties.getBucketName();

        // ========== 防错：检测 yml 是否被忽略 ==========
        if (secretId == null || secretId.trim().isEmpty()
            || secretKey == null || secretKey.trim().isEmpty()) {
            log.error("========== [致命错误] 配置缺失 ==========");
            log.error("❌ COS 密钥未配置！当前使用的 application.yml 可能有以下问题：");
            log.error("  1. 本地运行：请确认 src/main/resources/application-local.yml 是否存在（包含密钥）");
            log.error("  2. 服务器运行：请设置环境变量 COS_SECRET_ID 和 COS_SECRET_KEY");
            log.error("  3. Git 拉取后：application.yml 是否被 gitignore 忽略？请检查 git status");
            log.error("  4. jar 包内：检查 BOOT-INF/classes/application.yml 是否存在");
            log.error("");
            log.error("当前检测到的配置值：");
            log.error("  cos.secret-id:   {}", (secretId == null || secretId.isEmpty()) ? "❌ 空" : "✅ 已配置 (前缀: " + secretId.substring(0, Math.min(6, secretId.length())) + "...)");
            log.error("  cos.secret-key:  {}", (secretKey == null || secretKey.isEmpty()) ? "❌ 空" : "✅ 已配置 (长度: " + secretKey.length() + ")");
            log.error("  cos.region:      {}", (region == null || region.isEmpty()) ? "❌ 空" : "✅ " + region);
            log.error("  cos.bucket-name: {}", (bucket == null || bucket.isEmpty()) ? "❌ 空" : "✅ " + bucket);
            log.error("");
            log.error("💡 解决方案：服务器部署时执行：");
            log.error("   export COS_SECRET_ID=\"你的SecretId\"");
            log.error("   export COS_SECRET_KEY=\"你的SecretKey\"");
            log.error("   java -jar xiaxiang-building-tour.jar");
            log.error("==================================");
            throw new IllegalStateException(
                "COS 密钥缺失！请检查 application.yml 是否被忽略，或设置环境变量 COS_SECRET_ID/COS_SECRET_KEY。"
                + " 当前检测：secret-id=" + (secretId == null ? "null" : (secretId.isEmpty() ? "空" : "已配置"))
                + "，secret-key=" + (secretKey == null ? "null" : (secretKey.isEmpty() ? "空" : "已配置"))
            );
        }

        // ========== 配置正常，打印汇总 ==========
        log.info("[配置] 密钥检测通过：SecretId 前缀: {}，SecretKey 长度: {}",
                secretId.substring(0, Math.min(6, secretId.length())),
                secretKey.length());
        log.info("[配置] Region: {}, Bucket: {}", region, bucket);
        log.info("================================");

        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        this.cosClient = new COSClient(cred, clientConfig);

        log.info("[COS] COSClient 初始化完成");
        return this.cosClient;
    }

    /**
     * 应用关闭时释放 COSClient 连接池
     */
    @PreDestroy
    public void shutdown() {
        if (this.cosClient != null) {
            log.info("[COS] 正在关闭 COSClient，释放连接池...");
            this.cosClient.shutdown();
            log.info("[COS] COSClient 已安全关闭");
        }
    }
}
