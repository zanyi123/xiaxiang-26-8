package org.example.xiaxiang.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 腾讯云 COS 配置映射类
 * 与 application.yml 中的 cos.* 前缀配置绑定
 *
 * 防坑设计：
 * 1. secret-id / secret-key 为敏感信息，严禁硬编码或提交 Git
 * 2. 生产环境建议通过环境变量、K8s Secret 或 CI/CD 注入
 */
@Data
@ConfigurationProperties(prefix = "cos")
public class CosProperties {

    /** 腾讯云 API 密钥 ID */
    private String secretId;

    /** 腾讯云 API 密钥 Key */
    private String secretKey;

    /** COS 存储桶所在地域，如 ap-hongkong */
    private String region;

    /** COS 存储桶名称 */
    private String bucketName;
}
