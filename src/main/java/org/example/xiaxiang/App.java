package org.example.xiaxiang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.example.xiaxiang.properties.CosProperties;

/**
 * 江门侨乡建筑数字化云游平台 —— 启动类
 *
 * @author GDUT 侨韵薪火团队
 * @version 1.0
 * @date 2026-07-27
 */
@SpringBootApplication
@EnableConfigurationProperties(CosProperties.class)
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
