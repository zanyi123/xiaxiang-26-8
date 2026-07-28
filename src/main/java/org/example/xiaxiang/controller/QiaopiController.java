package org.example.xiaxiang.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.example.xiaxiang.properties.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 侨批文化展示模块
 * 展示华侨书信与汇款凭证，解读跨海家书背后的故事
 */
@Slf4j
@Controller
public class QiaopiController {

    @Autowired
    private AppProperties appProperties;

    @GetMapping("/qiaopi")
    public String qiaopiPage(@RequestParam(required = false) String category, Model model) {
        log.info("[QiaopiController] 访问侨批文化页，category={}", category);

        List<AppProperties.QiaopiItem> allItems = appProperties.getQiaopi();
        if (allItems == null) {
            allItems = Collections.emptyList();
        }

        List<String> categories = allItems.stream()
                .map(AppProperties.QiaopiItem::getCategory)
                .distinct()
                .collect(Collectors.toList());

        List<AppProperties.QiaopiItem> filtered = allItems;
        if (category != null && !category.trim().isEmpty() && !"all".equals(category)) {
            final String cat = category;
            filtered = allItems.stream()
                    .filter(q -> cat.equals(q.getCategory()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("qiaopiList", filtered);
        model.addAttribute("categories", categories);
        model.addAttribute("currentCategory", category != null ? category : "all");
        return "qiaopi";
    }

    @GetMapping("/qiaopi/{id}")
    public String qiaopiDetail(@PathVariable Integer id, Model model) {
        log.info("[QiaopiController] 访问侨批详情页，id={}", id);

        AppProperties.QiaopiItem item = findQiaopiById(id);
        if (item == null) {
            model.addAttribute("error", "侨批不存在");
            return "error";
        }
        model.addAttribute("qiaopi", item);

        List<AppProperties.QiaopiItem> others = appProperties.getQiaopi() == null
                ? Collections.emptyList()
                : appProperties.getQiaopi().stream()
                .filter(q -> !id.equals(q.getId()))
                .limit(3)
                .collect(Collectors.toList());
        model.addAttribute("otherQiaopi", others);

        return "qiaopi-detail";
    }

    @RestController
    @RequestMapping("/api/qiaopi")
    public static class QiaopiApiController {

        @Autowired
        private QiaopiController qiaopiController;

        @Autowired
        private AppProperties appProperties;

        @GetMapping
        public Result<List<AppProperties.QiaopiItem>> list(
                @RequestParam(required = false) String category) {
            log.info("[QiaopiApiController] 获取侨批列表，category={}", category);
            List<AppProperties.QiaopiItem> all = appProperties.getQiaopi();
            if (all == null) {
                return Result.success(Collections.emptyList());
            }
            if (category == null || category.trim().isEmpty() || "all".equals(category)) {
                return Result.success(all);
            }
            List<AppProperties.QiaopiItem> filtered = all.stream()
                    .filter(q -> category.equals(q.getCategory()))
                    .collect(Collectors.toList());
            return Result.success(filtered);
        }

        @GetMapping("/{id}")
        public Result<AppProperties.QiaopiItem> detail(@PathVariable Integer id) {
            log.info("[QiaopiApiController] 获取侨批详情，id={}", id);
            AppProperties.QiaopiItem item = qiaopiController.findQiaopiById(id);
            if (item == null) {
                return Result.fail("侨批不存在");
            }
            return Result.success(item);
        }

        @GetMapping("/categories")
        public Result<List<String>> categories() {
            log.info("[QiaopiApiController] 获取侨批分类列表");
            List<AppProperties.QiaopiItem> all = appProperties.getQiaopi();
            if (all == null) {
                return Result.success(Collections.emptyList());
            }
            return Result.success(all.stream()
                    .map(AppProperties.QiaopiItem::getCategory)
                    .distinct()
                    .collect(Collectors.toList()));
        }
    }

    AppProperties.QiaopiItem findQiaopiById(Integer id) {
        if (appProperties.getQiaopi() == null) {
            return null;
        }
        return appProperties.getQiaopi().stream()
                .filter(q -> id.equals(q.getId()))
                .findFirst()
                .orElse(null);
    }
}
