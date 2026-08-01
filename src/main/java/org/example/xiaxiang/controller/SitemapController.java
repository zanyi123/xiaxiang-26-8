package org.example.xiaxiang.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.properties.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 网站导览控制器
 * 按项目树分区列出所有二级入口，解决顶部栏按键过多问题
 */
@Slf4j
@Controller
public class SitemapController {

    @Autowired
    private AppProperties appProperties;

    @GetMapping("/sitemap")
    public String sitemap(Model model) {
        log.info("[SitemapController] 访问网站导览页");
        model.addAttribute("locations", appProperties.getLocations());
        model.addAttribute("buildings", appProperties.getBuildings());
        return "sitemap";
    }
}
