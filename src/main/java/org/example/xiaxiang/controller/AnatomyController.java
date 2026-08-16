package org.example.xiaxiang.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.example.xiaxiang.properties.AppProperties;
import org.example.xiaxiang.service.CosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 建筑解剖互动模块
 * 将碉楼拆解为多个部位，展示每个部分的功能、材料与年代
 */
@Slf4j
@Controller
public class AnatomyController {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private CosService cosService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/anatomy")
    public String anatomyPage(@RequestParam(required = false) Integer buildingId, Model model) {
        log.info("[AnatomyController] 访问建筑解剖页，buildingId={}", buildingId);

        Integer targetBuildingId = buildingId != null ? buildingId : 1;

        List<AppProperties.BuildingAnatomy> allParts = getAnatomiesByBuilding(targetBuildingId);
        model.addAttribute("buildingId", targetBuildingId);
        model.addAttribute("buildings", appProperties.getBuildings());
        model.addAttribute("parts", allParts);

        // 构建部位图片 URL Map（id -> COS URL），供前端图片展示
        model.addAttribute("imageUrls", cosService.buildUrlMap(allParts,
                AppProperties.BuildingAnatomy::getId, AppProperties.BuildingAnatomy::getImageKey));

        // 构建每个部位的知识点 JSON 字符串（供前端 JS 解析渲染）
        // knowledgePoints 在 YAML 中存储为 JSON 字符串，直接传递给前端
        Map<Integer, String> knowledgePointsJsonMap = new HashMap<>();
        for (AppProperties.BuildingAnatomy part : allParts) {
            if (part.getKnowledgePoints() != null && !part.getKnowledgePoints().trim().isEmpty()) {
                // 验证 JSON 格式是否正确
                String kpJson = part.getKnowledgePoints().trim();
                try {
                    objectMapper.readTree(kpJson);
                    // 转换知识点图片 key 为完整 COS URL
                    List<Map<String, String>> kpList = objectMapper.readValue(kpJson,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                    for (Map<String, String> kp : kpList) {
                        String imgKey = kp.get("imageKey");
                        if (imgKey != null && !imgKey.trim().isEmpty()) {
                            String imgUrl = cosService.getUrlSafely(imgKey);
                            kp.put("imageUrl", imgUrl != null ? imgUrl : "");
                        }
                    }
                    knowledgePointsJsonMap.put(part.getId(), objectMapper.writeValueAsString(kpList));
                } catch (JsonProcessingException e) {
                    log.warn("[AnatomyController] 知识点JSON格式错误，partId={}: {}", part.getId(), e.getMessage());
                    // 即使格式错误也原样传递，前端会显示空列表
                    knowledgePointsJsonMap.put(part.getId(), "[]");
                }
            }
        }
        model.addAttribute("knowledgePointsJsonMap", knowledgePointsJsonMap);

        return "anatomy";
    }

    @RestController
    @RequestMapping("/api/anatomy")
    public static class AnatomyApiController {

        @Autowired
        private AnatomyController anatomyController;

        @GetMapping
        public Result<List<AppProperties.BuildingAnatomy>> listByBuilding(
                @RequestParam(defaultValue = "1") Integer buildingId) {
            log.info("[AnatomyApiController] 获取建筑解剖列表，buildingId={}", buildingId);
            return Result.success(anatomyController.getAnatomiesByBuilding(buildingId));
        }

        @GetMapping("/{id}")
        public Result<AppProperties.BuildingAnatomy> detail(@PathVariable Integer id) {
            log.info("[AnatomyApiController] 获取解剖部位详情，id={}", id);
            AppProperties.BuildingAnatomy anatomy = anatomyController.findAnatomyById(id);
            if (anatomy == null) {
                return Result.fail("部位不存在");
            }
            return Result.success(anatomy);
        }

        @GetMapping("/categories")
        public Result<List<String>> categories() {
            log.info("[AnatomyApiController] 获取部位分类列表");
            return Result.success(anatomyController.getAllCategories());
        }
    }

    List<AppProperties.BuildingAnatomy> getAnatomiesByBuilding(Integer buildingId) {
        if (appProperties.getAnatomies() == null) {
            return Collections.emptyList();
        }
        return appProperties.getAnatomies().stream()
                .filter(a -> buildingId.equals(a.getBuildingId()))
                .collect(Collectors.toList());
    }

    AppProperties.BuildingAnatomy findAnatomyById(Integer id) {
        if (appProperties.getAnatomies() == null) {
            return null;
        }
        return appProperties.getAnatomies().stream()
                .filter(a -> id.equals(a.getId()))
                .findFirst()
                .orElse(null);
    }

    List<String> getAllCategories() {
        if (appProperties.getAnatomies() == null) {
            return Collections.emptyList();
        }
        return appProperties.getAnatomies().stream()
                .map(AppProperties.BuildingAnatomy::getCategory)
                .distinct()
                .collect(Collectors.toList());
    }
}
