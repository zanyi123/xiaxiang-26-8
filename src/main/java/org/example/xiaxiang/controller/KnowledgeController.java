package org.example.xiaxiang.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.example.xiaxiang.properties.AppProperties;
import org.example.xiaxiang.service.CosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class KnowledgeController {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private CosService cosService;

    @GetMapping("/knowledge")
    public String knowledge(Model model) {
        log.info("[KnowledgeController] 访问建筑知识库页");
        model.addAttribute("knowledgeList", appProperties.getKnowledge());
        model.addAttribute("cultures", appProperties.getCultures());
        model.addAttribute("locations", appProperties.getLocations());
        // 素材 URL Map
        model.addAttribute("coverUrls", cosService.buildUrlMap(appProperties.getKnowledge(), AppProperties.KnowledgeItem::getId, AppProperties.KnowledgeItem::getCoverImage));
        model.addAttribute("cultureCoverUrls", cosService.buildUrlMap(appProperties.getCultures(), AppProperties.CultureItem::getId, AppProperties.CultureItem::getCoverImage));
        return "knowledge";
    }

    @GetMapping("/knowledge/{id}")
    public String knowledgeDetail(@PathVariable Integer id, Model model) {
        log.info("[KnowledgeController] 访问知识详情页，ID={}", id);

        AppProperties.KnowledgeItem knowledge = findKnowledgeById(id);
        if (knowledge == null) {
            model.addAttribute("error", "知识条目不存在");
            return "error";
        }

        model.addAttribute("knowledge", knowledge);
        // 当前知识的封面图 URL
        model.addAttribute("coverUrl", cosService.getUrlSafely(knowledge.getCoverImage()));

        List<AppProperties.KnowledgeItem> relatedKnowledge = appProperties.getKnowledge().stream()
                .filter(k -> !k.getId().equals(id))
                .limit(3)
                .collect(Collectors.toList());
        model.addAttribute("relatedKnowledge", relatedKnowledge);
        // 相关知识的封面图 URL Map
        model.addAttribute("coverUrls", cosService.buildUrlMap(relatedKnowledge, AppProperties.KnowledgeItem::getId, AppProperties.KnowledgeItem::getCoverImage));

        return "knowledge-detail";
    }

    @RestController
    @RequestMapping("/api/knowledge")
    public static class KnowledgeApiController {

        @Autowired
        private AppProperties appProperties;

        @GetMapping
        public Result<List<AppProperties.KnowledgeItem>> getAllKnowledge() {
            log.info("[KnowledgeApiController] 获取所有知识列表");
            List<AppProperties.KnowledgeItem> knowledge = appProperties.getKnowledge();
            if (knowledge == null) {
                return Result.success(java.util.Collections.emptyList());
            }
            return Result.success(knowledge);
        }

        @GetMapping("/{id}")
        public Result<AppProperties.KnowledgeItem> getKnowledgeById(@PathVariable Integer id) {
            log.info("[KnowledgeApiController] 获取知识详情，ID={}", id);
            AppProperties.KnowledgeItem knowledge = appProperties.getKnowledge().stream()
                    .filter(k -> id.equals(k.getId()))
                    .findFirst()
                    .orElse(null);
            if (knowledge == null) {
                return Result.fail("知识条目不存在");
            }
            return Result.success(knowledge);
        }

        @GetMapping("/category/{category}")
        public Result<List<AppProperties.KnowledgeItem>> getKnowledgeByCategory(@PathVariable String category) {
            log.info("[KnowledgeApiController] 按分类获取知识，category={}", category);
            List<AppProperties.KnowledgeItem> result = appProperties.getKnowledge().stream()
                    .filter(k -> category.equals(k.getCategory()))
                    .collect(Collectors.toList());
            return Result.success(result);
        }

        @GetMapping("/cultures")
        public Result<List<AppProperties.CultureItem>> getAllCultures() {
            log.info("[KnowledgeApiController] 获取所有民俗文化");
            List<AppProperties.CultureItem> cultures = appProperties.getCultures();
            if (cultures == null) {
                return Result.success(java.util.Collections.emptyList());
            }
            return Result.success(cultures);
        }
    }

    private AppProperties.KnowledgeItem findKnowledgeById(Integer id) {
        if (id == null || appProperties.getKnowledge() == null) {
            return null;
        }
        return appProperties.getKnowledge().stream()
                .filter(k -> id.equals(k.getId()))
                .findFirst()
                .orElse(null);
    }
}
