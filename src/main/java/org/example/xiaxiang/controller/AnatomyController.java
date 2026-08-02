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

    @GetMapping("/anatomy")
    public String anatomyPage(@RequestParam(required = false) Integer buildingId, Model model) {
        log.info("[AnatomyController] 访问建筑解剖页，buildingId={}", buildingId);

        Integer targetBuildingId = buildingId != null ? buildingId : 1;

        List<AppProperties.BuildingAnatomy> allParts = getAnatomiesByBuilding(targetBuildingId);
        model.addAttribute("buildingId", targetBuildingId);
        model.addAttribute("buildings", appProperties.getBuildings());
        model.addAttribute("parts", allParts);

        // 构建部位 3D 模型 URL Map（id -> COS URL），供前端 3D 解剖展示使用
        model.addAttribute("modelUrls", cosService.buildUrlMap(allParts,
                AppProperties.BuildingAnatomy::getId, AppProperties.BuildingAnatomy::getModelKey));
        // 构建部位图片 URL Map（id -> COS URL），作为 3D 模型未上传时的 fallback
        model.addAttribute("imageUrls", cosService.buildUrlMap(allParts,
                AppProperties.BuildingAnatomy::getId, AppProperties.BuildingAnatomy::getImageKey));
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
