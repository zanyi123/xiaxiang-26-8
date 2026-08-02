package org.example.xiaxiang.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.example.xiaxiang.properties.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class SearchController {

    @Autowired
    private AppProperties appProperties;

    @GetMapping("/search")
    public String searchPage(@RequestParam String keyword, Model model) {
        log.info("[SearchController] 访问搜索结果页，关键词={}", keyword);

        Map<String, Object> results = doSearch(keyword);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("results", results);
        return "search-result";
    }

    @RestController
    @RequestMapping("/api/search")
    public static class SearchApiController {

        @Autowired
        private SearchController searchController;

        @GetMapping
        public Result<Map<String, Object>> search(@RequestParam String keyword) {
            log.info("[SearchApiController] 搜索，关键词={}", keyword);
            return Result.success(searchController.doSearch(keyword));
        }

        @GetMapping("/suggest")
        public Result<List<Map<String, Object>>> suggest(@RequestParam String keyword) {
            log.info("[SearchApiController] 搜索建议，关键词={}", keyword);
            return Result.success(searchController.doSuggest(keyword));
        }
    }

    Map<String, Object> doSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("total", 0);
            empty.put("keyword", "");
            empty.put("items", new ArrayList<>());
            return empty;
        }

        String kw = keyword.trim().toLowerCase();
        List<Map<String, Object>> items = new ArrayList<>();

        if (appProperties.getLocations() != null) {
            appProperties.getLocations().stream()
                    .filter(l -> (l.getName() != null && l.getName().toLowerCase().contains(kw))
                            || (l.getDescription() != null && l.getDescription().toLowerCase().contains(kw)))
                    .forEach(loc -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("type", "location");
                        item.put("id", loc.getId());
                        item.put("title", loc.getName());
                        item.put("description", loc.getDescription());
                        item.put("url", "/location/" + loc.getId());
                        items.add(item);
                    });
        }

        if (appProperties.getStories() != null) {
            appProperties.getStories().stream()
                    .filter(s -> (s.getTitle() != null && s.getTitle().toLowerCase().contains(kw))
                            || (s.getSummary() != null && s.getSummary().toLowerCase().contains(kw)))
                    .forEach(story -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("type", "story");
                        item.put("id", story.getId());
                        item.put("title", story.getTitle());
                        item.put("description", story.getSummary());
                        item.put("url", "/story/" + story.getId());
                        item.put("category", story.getCategory());
                        items.add(item);
                    });
        }

        if (appProperties.getKnowledge() != null) {
            appProperties.getKnowledge().stream()
                    .filter(k -> (k.getTitle() != null && k.getTitle().toLowerCase().contains(kw))
                            || (k.getSummary() != null && k.getSummary().toLowerCase().contains(kw)))
                    .forEach(k -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("type", "knowledge");
                        item.put("id", k.getId());
                        item.put("title", k.getTitle());
                        item.put("description", k.getSummary());
                        item.put("url", "/knowledge/" + k.getId());
                        item.put("category", k.getCategory());
                        items.add(item);
                    });
        }

        if (appProperties.getArchives() != null) {
            appProperties.getArchives().stream()
                    .filter(a -> (a.getTitle() != null && a.getTitle().toLowerCase().contains(kw))
                            || (a.getDescription() != null && a.getDescription().toLowerCase().contains(kw))
                            || (a.getCategory() != null && a.getCategory().toLowerCase().contains(kw)))
                    .forEach(a -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("type", "archive");
                        item.put("id", a.getId());
                        item.put("title", a.getTitle());
                        item.put("description", a.getDescription());
                        item.put("url", "/archive/" + a.getId());
                        item.put("category", a.getCategory());
                        items.add(item);
                    });
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", items.size());
        result.put("keyword", keyword);
        result.put("items", items);
        return result;
    }

    List<Map<String, Object>> doSuggest(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String kw = keyword.trim().toLowerCase();
        List<Map<String, Object>> suggestions = new ArrayList<>();

        if (appProperties.getLocations() != null) {
            appProperties.getLocations().stream()
                    .filter(l -> l.getName() != null && l.getName().toLowerCase().contains(kw))
                    .limit(3)
                    .forEach(l -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("type", "地点");
                        item.put("text", l.getName());
                        suggestions.add(item);
                    });
        }

        if (appProperties.getStories() != null) {
            appProperties.getStories().stream()
                    .filter(s -> s.getTitle() != null && s.getTitle().toLowerCase().contains(kw))
                    .limit(3)
                    .forEach(s -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("type", "故事");
                        item.put("text", s.getTitle());
                        suggestions.add(item);
                    });
        }

        if (appProperties.getKnowledge() != null) {
            appProperties.getKnowledge().stream()
                    .filter(k -> k.getTitle() != null && k.getTitle().toLowerCase().contains(kw))
                    .limit(3)
                    .forEach(k -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("type", "知识");
                        item.put("text", k.getTitle());
                        suggestions.add(item);
                    });
        }

        return suggestions;
    }
}
