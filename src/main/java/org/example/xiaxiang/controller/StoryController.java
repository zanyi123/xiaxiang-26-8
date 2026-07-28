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
public class StoryController {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private CosService cosService;

    @GetMapping("/stories")
    public String stories(Model model) {
        log.info("[StoryController] 访问侨乡故事列表页");
        model.addAttribute("stories", appProperties.getStories());
        model.addAttribute("locations", appProperties.getLocations());
        // 素材 URL Map（封面图 + 音频）
        model.addAttribute("coverUrls", cosService.buildUrlMap(appProperties.getStories(), AppProperties.Story::getId, AppProperties.Story::getCoverImage));
        model.addAttribute("audioUrls", cosService.buildUrlMap(appProperties.getStories(), AppProperties.Story::getId, AppProperties.Story::getAudioKey));
        return "stories";
    }

    @GetMapping("/story/{id}")
    public String storyDetail(@PathVariable Integer id, Model model) {
        log.info("[StoryController] 访问故事详情页，ID={}", id);

        AppProperties.Story story = findStoryById(id);
        if (story == null) {
            model.addAttribute("error", "故事不存在");
            return "error";
        }

        model.addAttribute("story", story);
        // 当前故事的封面图与音频 URL
        model.addAttribute("coverUrl", cosService.getUrlSafely(story.getCoverImage()));
        model.addAttribute("audioUrl", cosService.getUrlSafely(story.getAudioKey()));

        List<AppProperties.Story> otherStories = appProperties.getStories().stream()
                .filter(s -> !s.getId().equals(id))
                .limit(3)
                .collect(Collectors.toList());
        model.addAttribute("otherStories", otherStories);
        // 相关故事的封面图 URL Map
        model.addAttribute("coverUrls", cosService.buildUrlMap(otherStories, AppProperties.Story::getId, AppProperties.Story::getCoverImage));

        return "story-detail";
    }

    @RestController
    @RequestMapping("/api/stories")
    public static class StoryApiController {

        @Autowired
        private AppProperties appProperties;

        @GetMapping
        public Result<List<AppProperties.Story>> getAllStories() {
            log.info("[StoryApiController] 获取所有故事列表");
            List<AppProperties.Story> stories = appProperties.getStories();
            if (stories == null) {
                return Result.success(java.util.Collections.emptyList());
            }
            return Result.success(stories);
        }

        @GetMapping("/{id}")
        public Result<AppProperties.Story> getStoryById(@PathVariable Integer id) {
            log.info("[StoryApiController] 获取故事详情，ID={}", id);
            AppProperties.Story story = appProperties.getStories().stream()
                    .filter(s -> id.equals(s.getId()))
                    .findFirst()
                    .orElse(null);
            if (story == null) {
                return Result.fail("故事不存在");
            }
            return Result.success(story);
        }

        @GetMapping("/category/{category}")
        public Result<List<AppProperties.Story>> getStoriesByCategory(@PathVariable String category) {
            log.info("[StoryApiController] 按分类获取故事，category={}", category);
            List<AppProperties.Story> result = appProperties.getStories().stream()
                    .filter(s -> category.equals(s.getCategory()))
                    .collect(Collectors.toList());
            return Result.success(result);
        }
    }

    private AppProperties.Story findStoryById(Integer id) {
        if (id == null || appProperties.getStories() == null) {
            return null;
        }
        return appProperties.getStories().stream()
                .filter(s -> id.equals(s.getId()))
                .findFirst()
                .orElse(null);
    }
}
