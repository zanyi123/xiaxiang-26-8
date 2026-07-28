package org.example.xiaxiang.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.properties.AppProperties;
import org.example.xiaxiang.service.CosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 页面路由控制器
 *
 * 负责：首页、建筑详情页、景区导航页的路由分发
 * 数据传递：通过 Model 将 COS URL 传递给 Thymeleaf 模板
 */
@Slf4j
@Controller
public class IndexController {

    @Autowired
    private CosService cosService;

    @Autowired
    private AppProperties appProperties;

    /**
     * 首页：扫码入口页
     */
    @GetMapping("/")
    public String index(Model model) {
        log.info("[IndexController] 访问首页（扫码入口页）");
        model.addAttribute("buildings", appProperties.getBuildings());
        model.addAttribute("locations", appProperties.getLocations());
        model.addAttribute("stories", appProperties.getStories());
        model.addAttribute("timeline", appProperties.getTimeline());
        model.addAttribute("archives", appProperties.getArchives());
        model.addAttribute("knowledgeList", appProperties.getKnowledge());
        model.addAttribute("cultures", appProperties.getCultures());
        model.addAttribute("blogPosts", appProperties.getBlogPosts());
        model.addAttribute("videos", appProperties.getVideos());
        model.addAttribute("team", appProperties.getTeam());

        // 素材 URL Map：key=对象 id, value=COS URL（mock 模式下为 /mock/xxx，真实模式为完整 COS URL）
        // 模板中可通过 ${coverUrls[id]} 访问，为 null 时回退到 AI 生成图
        model.addAttribute("buildingCoverUrls", buildUrlMap(appProperties.getBuildings(), AppProperties.Building::getId, AppProperties.Building::getCoverImage));
        model.addAttribute("buildingModelUrls", buildUrlMap(appProperties.getBuildings(), AppProperties.Building::getId, AppProperties.Building::getModelKey));
        model.addAttribute("buildingVideoUrls", buildUrlMap(appProperties.getBuildings(), AppProperties.Building::getId, AppProperties.Building::getVideoKey));
        model.addAttribute("locationImageUrls", buildUrlMap(appProperties.getLocations(), AppProperties.Location::getId, AppProperties.Location::getImageKey));
        model.addAttribute("locationModelUrls", buildUrlMap(appProperties.getLocations(), AppProperties.Location::getId, AppProperties.Location::getModelKey));
        model.addAttribute("storyCoverUrls", buildUrlMap(appProperties.getStories(), AppProperties.Story::getId, AppProperties.Story::getCoverImage));
        model.addAttribute("storyAudioUrls", buildUrlMap(appProperties.getStories(), AppProperties.Story::getId, AppProperties.Story::getAudioKey));
        model.addAttribute("knowledgeCoverUrls", buildUrlMap(appProperties.getKnowledge(), AppProperties.KnowledgeItem::getId, AppProperties.KnowledgeItem::getCoverImage));
        model.addAttribute("blogCoverUrls", buildUrlMap(appProperties.getBlogPosts(), AppProperties.BlogPost::getId, AppProperties.BlogPost::getCoverImage));
        model.addAttribute("videoCoverUrls", buildUrlMap(appProperties.getVideos(), AppProperties.VideoItem::getId, AppProperties.VideoItem::getCoverImage));
        model.addAttribute("videoFileUrls", buildUrlMap(appProperties.getVideos(), AppProperties.VideoItem::getId, AppProperties.VideoItem::getVideoKey));
        model.addAttribute("cultureCoverUrls", buildUrlMap(appProperties.getCultures(), AppProperties.CultureItem::getId, AppProperties.CultureItem::getCoverImage));
        model.addAttribute("teamAvatarUrls", buildUrlMap(appProperties.getTeam(), AppProperties.TeamMember::getId, AppProperties.TeamMember::getAvatar));

        return "index";
    }

    /**
     * 通用素材 URL Map 构造器（委托给 CosService）
     *
     * @param list     数据列表
     * @param idGetter id 提取函数
     * @param keyGetter 素材 key 提取函数
     * @return Map<id, COS URL>，URL 为 null 时不出现在 Map 中
     */
    private <T> Map<Integer, String> buildUrlMap(List<T> list,
                                                 java.util.function.Function<T, Integer> idGetter,
                                                 java.util.function.Function<T, String> keyGetter) {
        return cosService.buildUrlMap(list, idGetter, keyGetter);
    }

    /**
     * 景区导航页：核心导航页面
     */
    @GetMapping("/map")
    public String map(Model model) {
        log.info("[IndexController] 访问景区导航页");
        model.addAttribute("locations", appProperties.getLocations());
        return "map";
    }

    /**
     * 建筑详情页：展示单个建筑的 3D 模型和视频
     *
     * @param id 建筑 ID
     */
    @GetMapping("/building/{id}")
    public String buildingDetail(@PathVariable Integer id, Model model) {
        log.info("[IndexController] 访问建筑详情页，ID={}", id);

        AppProperties.Building building = cosService.findBuildingById(id);
        if (building == null) {
            log.warn("[IndexController] 建筑不存在，ID={}", id);
            model.addAttribute("error", "建筑不存在");
            return "error";
        }

        String modelUrl = cosService.getModelUrl(id);
        String videoUrl = cosService.getVideoUrl(id);
        String coverImageUrl = cosService.getCoverImageUrl(id);

        model.addAttribute("building", building);
        model.addAttribute("modelUrl", modelUrl);
        model.addAttribute("videoUrl", videoUrl);
        model.addAttribute("coverImageUrl", coverImageUrl);

        log.info("[IndexController] 建筑详情页数据已准备，ID={}, modelUrl={}", id, modelUrl);
        return "building";
    }

    /**
     * 地点详情页：展示单个地点的 3D 模型和 AI 讲解
     *
     * @param id 地点 ID
     */
    @GetMapping("/location/{id}")
    public String locationDetail(@PathVariable Integer id, Model model) {
        log.info("[IndexController] 访问地点详情页，ID={}", id);

        AppProperties.Location location = cosService.findLocationById(id);
        if (location == null) {
            log.warn("[IndexController] 地点不存在，ID={}", id);
            model.addAttribute("error", "地点不存在");
            return "error";
        }

        String modelUrl = cosService.getLocationModelUrl(id);
        String imageUrl = cosService.getLocationImageUrl(id);

        model.addAttribute("location", location);
        model.addAttribute("modelUrl", modelUrl);
        model.addAttribute("imageUrl", imageUrl);

        List<AppProperties.Location> otherLocations = appProperties.getLocations().stream()
                .filter(l -> !l.getId().equals(id))
                .collect(Collectors.toList());
        model.addAttribute("otherLocations", otherLocations);

        log.info("[IndexController] 地点详情页数据已准备，ID={}, modelUrl={}", id, modelUrl);
        return "location";
    }
}