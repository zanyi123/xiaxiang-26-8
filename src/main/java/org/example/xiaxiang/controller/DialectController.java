package org.example.xiaxiang.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.example.xiaxiang.properties.AppProperties;
import org.example.xiaxiang.service.CosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 方言学习互动模块
 * 侨乡方言词条学习，含发音、释义与例句
 */
@Slf4j
@Controller
public class DialectController {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private CosService cosService;

    @GetMapping("/dialect")
    public String dialectPage(@RequestParam(required = false) String category, Model model) {
        log.info("[DialectController] 访问方言学习页，category={}", category);

        List<AppProperties.DialectItem> all = appProperties.getDialects();
        if (all == null) {
            all = Collections.emptyList();
        }

        List<String> categories = all.stream()
                .map(AppProperties.DialectItem::getCategory)
                .distinct()
                .collect(Collectors.toList());

        List<AppProperties.DialectItem> filtered = all;
        if (category != null && !category.trim().isEmpty() && !"all".equals(category)) {
            final String cat = category;
            filtered = all.stream()
                    .filter(d -> cat.equals(d.getCategory()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("dialects", filtered);
        model.addAttribute("categories", categories);
        model.addAttribute("currentCategory", category != null ? category : "all");
        // 构建方言音频 URL Map（id -> COS URL），供前端优先播放真实录音
        model.addAttribute("audioUrls", cosService.buildUrlMap(filtered, AppProperties.DialectItem::getId, AppProperties.DialectItem::getAudioKey));
        return "dialect";
    }

    @RestController
    @RequestMapping("/api/dialect")
    public static class DialectApiController {

        @Autowired
        private DialectController dialectController;

        @Autowired
        private AppProperties appProperties;

        @GetMapping
        public Result<List<AppProperties.DialectItem>> list(
                @RequestParam(required = false) String category) {
            log.info("[DialectApiController] 获取方言列表，category={}", category);
            List<AppProperties.DialectItem> all = appProperties.getDialects();
            if (all == null) {
                return Result.success(Collections.emptyList());
            }
            if (category == null || category.trim().isEmpty() || "all".equals(category)) {
                return Result.success(all);
            }
            return Result.success(all.stream()
                    .filter(d -> category.equals(d.getCategory()))
                    .collect(Collectors.toList()));
        }

        @GetMapping("/{id}")
        public Result<AppProperties.DialectItem> detail(@PathVariable Integer id) {
            log.info("[DialectApiController] 获取方言详情，id={}", id);
            AppProperties.DialectItem item = dialectController.findDialectById(id);
            if (item == null) {
                return Result.fail("词条不存在");
            }
            return Result.success(item);
        }

        @GetMapping("/categories")
        public Result<List<String>> categories() {
            log.info("[DialectApiController] 获取方言分类列表");
            List<AppProperties.DialectItem> all = appProperties.getDialects();
            if (all == null) {
                return Result.success(Collections.emptyList());
            }
            return Result.success(all.stream()
                    .map(AppProperties.DialectItem::getCategory)
                    .distinct()
                    .collect(Collectors.toList()));
        }

        @GetMapping("/random")
        public Result<AppProperties.DialectItem> random() {
            log.info("[DialectApiController] 随机获取一条方言");
            List<AppProperties.DialectItem> all = appProperties.getDialects();
            if (all == null || all.isEmpty()) {
                return Result.fail("暂无方言数据");
            }
            int idx = (int) (Math.random() * all.size());
            return Result.success(all.get(idx));
        }
    }

    AppProperties.DialectItem findDialectById(Integer id) {
        if (appProperties.getDialects() == null) {
            return null;
        }
        return appProperties.getDialects().stream()
                .filter(d -> id.equals(d.getId()))
                .findFirst()
                .orElse(null);
    }
}
