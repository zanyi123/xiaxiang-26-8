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

@Slf4j
@Controller
public class ArchiveController {

    @Autowired
    private AppProperties appProperties;

    @GetMapping("/archive")
    public String archive(Model model) {
        log.info("[ArchiveController] 访问田野档案页");
        model.addAttribute("archives", appProperties.getArchives());
        model.addAttribute("timeline", appProperties.getTimeline());
        model.addAttribute("locations", appProperties.getLocations());
        return "archive";
    }

    @GetMapping("/archive/{id}")
    public String archiveDetail(@PathVariable Integer id, Model model) {
        log.info("[ArchiveController] 访问档案详情页，ID={}", id);

        AppProperties.ArchiveItem item = findArchiveById(id);
        if (item == null) {
            log.warn("[ArchiveController] 档案不存在，ID={}", id);
            model.addAttribute("error", "档案不存在");
            return "error";
        }

        model.addAttribute("archive", item);

        // 同分类的其他档案（相关推荐）
        List<AppProperties.ArchiveItem> related = appProperties.getArchives() == null
                ? Collections.emptyList()
                : appProperties.getArchives().stream()
                .filter(a -> !id.equals(a.getId()))
                .limit(4)
                .collect(Collectors.toList());
        model.addAttribute("relatedArchives", related);

        // 该分类下的所有档案
        List<AppProperties.ArchiveItem> sameCategory = appProperties.getArchives() == null
                ? Collections.emptyList()
                : appProperties.getArchives().stream()
                .filter(a -> item.getCategory() != null && item.getCategory().equals(a.getCategory()))
                .collect(Collectors.toList());
        model.addAttribute("sameCategoryArchives", sameCategory);

        // 注入时间线数据（侧栏展示）
        model.addAttribute("timeline", appProperties.getTimeline());

        return "archive-detail";
    }

    @RestController
    @RequestMapping("/api/archive")
    public static class ArchiveApiController {

        @Autowired
        private AppProperties appProperties;

        @Autowired
        private ArchiveController archiveController;

        @GetMapping("/items")
        public Result<List<AppProperties.ArchiveItem>> getAllArchives() {
            log.info("[ArchiveApiController] 获取所有档案列表");
            List<AppProperties.ArchiveItem> archives = appProperties.getArchives();
            if (archives == null) {
                return Result.success(Collections.emptyList());
            }
            return Result.success(archives);
        }

        @GetMapping("/{id}")
        public Result<AppProperties.ArchiveItem> getArchiveById(@PathVariable Integer id) {
            log.info("[ArchiveApiController] 获取档案详情，ID={}", id);
            AppProperties.ArchiveItem item = archiveController.findArchiveById(id);
            if (item == null) {
                return Result.fail("档案不存在");
            }
            return Result.success(item);
        }

        @GetMapping("/timeline")
        public Result<List<AppProperties.TimelineItem>> getTimeline() {
            log.info("[ArchiveApiController] 获取历史时间线");
            List<AppProperties.TimelineItem> timeline = appProperties.getTimeline();
            if (timeline == null) {
                return Result.success(Collections.emptyList());
            }
            return Result.success(timeline);
        }

        @GetMapping("/category/{category}")
        public Result<List<AppProperties.ArchiveItem>> getArchivesByCategory(@PathVariable String category) {
            log.info("[ArchiveApiController] 按分类获取档案，category={}", category);
            List<AppProperties.ArchiveItem> result = appProperties.getArchives().stream()
                    .filter(a -> category.equals(a.getCategory()))
                    .collect(Collectors.toList());
            return Result.success(result);
        }
    }

    /**
     * 根据 ID 查找档案
     */
    public AppProperties.ArchiveItem findArchiveById(Integer id) {
        if (id == null || appProperties.getArchives() == null) {
            return null;
        }
        return appProperties.getArchives().stream()
                .filter(a -> id.equals(a.getId()))
                .findFirst()
                .orElse(null);
    }
}
