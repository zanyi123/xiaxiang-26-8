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
     */
    @Bean
    public COSClient cosClient() {
        log.info("[COS] 正在初始化 COSClient，Region={}", cosProperties.getRegion());

        COSCredentials cred = new BasicCOSCredentials(
                cosProperties.getSecretId(),
                cosProperties.getSecretKey()
        );

        ClientConfig clientConfig = new ClientConfig(new Region(cosProperties.getRegion()));
        this.cosClient = new COSClient(cred, clientConfig);

        log.info("[COS] COSClient 初始化完成，Bucket={}", cosProperties.getBucketName());
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
