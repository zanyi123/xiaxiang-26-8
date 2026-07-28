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
public class AboutController {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private CosService cosService;

    @GetMapping("/about")
    public String about(Model model) {
        log.info("[AboutController] 访问关于我们页");
        model.addAttribute("team", appProperties.getTeam());
        model.addAttribute("blogPosts", appProperties.getBlogPosts());
        model.addAttribute("videos", appProperties.getVideos());
        model.addAttribute("locations", appProperties.getLocations());
        // 素材 URL Map
        model.addAttribute("blogCoverUrls", cosService.buildUrlMap(appProperties.getBlogPosts(), AppProperties.BlogPost::getId, AppProperties.BlogPost::getCoverImage));
        model.addAttribute("videoCoverUrls", cosService.buildUrlMap(appProperties.getVideos(), AppProperties.VideoItem::getId, AppProperties.VideoItem::getCoverImage));
        model.addAttribute("videoFileUrls", cosService.buildUrlMap(appProperties.getVideos(), AppProperties.VideoItem::getId, AppProperties.VideoItem::getVideoKey));
        model.addAttribute("teamAvatarUrls", cosService.buildUrlMap(appProperties.getTeam(), AppProperties.TeamMember::getId, AppProperties.TeamMember::getAvatar));
        return "about";
    }

    @GetMapping("/blog/{id}")
    public String blogDetail(@PathVariable Integer id, Model model) {
        log.info("[AboutController] 访问实践日志详情页，ID={}", id);

        if (appProperties.getBlogPosts() == null) {
            model.addAttribute("error", "日志不存在");
            return "error";
        }

        AppProperties.BlogPost post = appProperties.getBlogPosts().stream()
                .filter(b -> id.equals(b.getId()))
                .findFirst()
                .orElse(null);

        if (post == null) {
            model.addAttribute("error", "日志不存在");
            return "error";
        }

        model.addAttribute("post", post);
        // 当前日志的封面图 URL
        model.addAttribute("coverUrl", cosService.getUrlSafely(post.getCoverImage()));

        List<AppProperties.BlogPost> otherPosts = appProperties.getBlogPosts().stream()
                .filter(b -> !b.getId().equals(id))
                .limit(3)
                .collect(Collectors.toList());
        model.addAttribute("otherPosts", otherPosts);
        // 相关日志的封面图 URL Map
        model.addAttribute("coverUrls", cosService.buildUrlMap(otherPosts, AppProperties.BlogPost::getId, AppProperties.BlogPost::getCoverImage));

        return "blog-detail";
    }

    @GetMapping("/video/{id}")
    public String videoDetail(@PathVariable Integer id, Model model) {
        log.info("[AboutController] 访问视频详情页，ID={}", id);

        if (appProperties.getVideos() == null) {
            model.addAttribute("error", "视频不存在");
            return "error";
        }

        AppProperties.VideoItem video = appProperties.getVideos().stream()
                .filter(v -> id.equals(v.getId()))
                .findFirst()
                .orElse(null);

        if (video == null) {
            model.addAttribute("error", "视频不存在");
            return "error";
        }

        model.addAttribute("video", video);
        // 当前视频的封面图与视频文件 URL
        model.addAttribute("coverUrl", cosService.getUrlSafely(video.getCoverImage()));
        model.addAttribute("videoUrl", cosService.getUrlSafely(video.getVideoKey()));

        List<AppProperties.VideoItem> otherVideos = appProperties.getVideos().stream()
                .filter(v -> !v.getId().equals(id))
                .limit(3)
                .collect(Collectors.toList());
        model.addAttribute("otherVideos", otherVideos);
        // 相关视频的封面图 URL Map
        model.addAttribute("coverUrls", cosService.buildUrlMap(otherVideos, AppProperties.VideoItem::getId, AppProperties.VideoItem::getCoverImage));

        return "video-detail";
    }

    @RestController
    @RequestMapping("/api/about")
    public static class AboutApiController {

        @Autowired
        private AppProperties appProperties;

        @GetMapping("/team")
        public Result<List<AppProperties.TeamMember>> getTeam() {
            log.info("[AboutApiController] 获取团队成员列表");
            List<AppProperties.TeamMember> team = appProperties.getTeam();
            if (team == null) {
                return Result.success(java.util.Collections.emptyList());
            }
            return Result.success(team);
        }

        @GetMapping("/blog")
        public Result<List<AppProperties.BlogPost>> getBlogPosts() {
            log.info("[AboutApiController] 获取实践日志列表");
            List<AppProperties.BlogPost> posts = appProperties.getBlogPosts();
            if (posts == null) {
                return Result.success(java.util.Collections.emptyList());
            }
            return Result.success(posts);
        }

        @GetMapping("/blog/{id}")
        public Result<AppProperties.BlogPost> getBlogById(@PathVariable Integer id) {
            log.info("[AboutApiController] 获取实践日志详情，ID={}", id);
            if (appProperties.getBlogPosts() == null) {
                return Result.fail("日志不存在");
            }
            AppProperties.BlogPost post = appProperties.getBlogPosts().stream()
                    .filter(b -> id.equals(b.getId()))
                    .findFirst()
                    .orElse(null);
            if (post == null) {
                return Result.fail("日志不存在");
            }
            return Result.success(post);
        }

        @GetMapping("/videos")
        public Result<List<AppProperties.VideoItem>> getVideos() {
            log.info("[AboutApiController] 获取视频列表");
            List<AppProperties.VideoItem> videos = appProperties.getVideos();
            if (videos == null) {
                return Result.success(java.util.Collections.emptyList());
            }
            return Result.success(videos);
        }

        @GetMapping("/video/{id}")
        public Result<AppProperties.VideoItem> getVideoById(@PathVariable Integer id) {
            log.info("[AboutApiController] 获取视频详情，ID={}", id);
            if (appProperties.getVideos() == null) {
                return Result.fail("视频不存在");
            }
            AppProperties.VideoItem video = appProperties.getVideos().stream()
                    .filter(v -> id.equals(v.getId()))
                    .findFirst()
                    .orElse(null);
            if (video == null) {
                return Result.fail("视频不存在");
            }
            return Result.success(video);
        }
    }
}
