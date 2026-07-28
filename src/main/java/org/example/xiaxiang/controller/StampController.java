package org.example.xiaxiang.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.example.xiaxiang.properties.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 虚拟盖章系统
 * 云游打卡收集印章，含稀有度与解锁条件
 * 进度存储于浏览器 localStorage，后端仅提供印章清单与解锁校验
 */
@Slf4j
@Controller
public class StampController {

    @Autowired
    private AppProperties appProperties;

    @GetMapping("/stamps")
    public String stampsPage(Model model) {
        log.info("[StampController] 访问虚拟盖章页");
        model.addAttribute("stamps", appProperties.getStamps());
        return "stamps";
    }

    @RestController
    @RequestMapping("/api/stamps")
    public static class StampApiController {

        @Autowired
        private StampController stampController;

        @Autowired
        private AppProperties appProperties;

        @GetMapping
        public Result<List<AppProperties.StampItem>> list() {
            log.info("[StampApiController] 获取印章列表");
            List<AppProperties.StampItem> stamps = appProperties.getStamps();
            if (stamps == null) {
                return Result.success(Collections.emptyList());
            }
            return Result.success(stamps);
        }

        @GetMapping("/{id}")
        public Result<AppProperties.StampItem> detail(@PathVariable Integer id) {
            log.info("[StampApiController] 获取印章详情，id={}", id);
            AppProperties.StampItem stamp = stampController.findStampById(id);
            if (stamp == null) {
                return Result.fail("印章不存在");
            }
            return Result.success(stamp);
        }

        @GetMapping("/unlocked")
        public Result<Map<String, Object>> unlockedStatus(@RequestParam String userId) {
            log.info("[StampApiController] 查询用户已解锁印章，userId={}", userId);
            // Mock：返回空列表，实际应由 localStorage 维护
            Map<String, Object> r = new HashMap<>();
            r.put("userId", userId);
            r.put("unlockedIds", Collections.emptyList());
            r.put("total", appProperties.getStamps() == null ? 0 : appProperties.getStamps().size());
            r.put("message", "印章进度由浏览器本地存储维护，请通过前端 API 更新");
            return Result.success(r);
        }

        @PostMapping("/unlock")
        public Result<Map<String, Object>> unlock(@RequestBody UnlockRequest request) {
            log.info("[StampController] 解锁印章，userId={}, stampId={}",
                    request.getUserId(), request.getStampId());

            AppProperties.StampItem stamp = stampController.findStampById(request.getStampId());
            if (stamp == null) {
                return Result.fail("印章不存在");
            }

            Map<String, Object> r = new HashMap<>();
            r.put("success", true);
            r.put("stampId", stamp.getId());
            r.put("stampName", stamp.getName());
            r.put("rarity", stamp.getRarity());
            r.put("message", "恭喜解锁印章：" + stamp.getName());
            return Result.success(r);
        }

        @GetMapping("/rarity/{rarity}")
        public Result<List<AppProperties.StampItem>> byRarity(@PathVariable String rarity) {
            log.info("[StampApiController] 按稀有度查询印章，rarity={}", rarity);
            List<AppProperties.StampItem> all = appProperties.getStamps();
            if (all == null) {
                return Result.success(Collections.emptyList());
            }
            return Result.success(all.stream()
                    .filter(s -> rarity.equals(s.getRarity()))
                    .collect(Collectors.toList()));
        }
    }

    AppProperties.StampItem findStampById(Integer id) {
        if (appProperties.getStamps() == null) {
            return null;
        }
        return appProperties.getStamps().stream()
                .filter(s -> id.equals(s.getId()))
                .findFirst()
                .orElse(null);
    }

    @lombok.Data
    public static class UnlockRequest {
        private String userId;
        private Integer stampId;
    }
}
