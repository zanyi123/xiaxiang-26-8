package org.example.xiaxiang.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.example.xiaxiang.exception.BusinessException;
import org.example.xiaxiang.properties.AppProperties;
import org.example.xiaxiang.service.CosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 地点管理控制器（景区导航用）
 *
 * 提供：地点列表、地点详情、模型URL、图片URL等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/locations")
public class LocationController {

    @Autowired
    private CosService cosService;

    /**
     * 获取所有地点列表
     */
    @GetMapping
    public Result<List<AppProperties.Location>> getAllLocations() {
        log.info("[LocationController] 获取所有地点列表");
        List<AppProperties.Location> locations = cosService.getAllLocations();
        return Result.success(locations);
    }

    /**
     * 获取单个地点详情
     */
    @GetMapping("/{id}")
    public Result<AppProperties.Location> getLocationById(@PathVariable Integer id) {
        log.info("[LocationController] 获取地点详情，ID={}", id);
        AppProperties.Location location = cosService.findLocationById(id);
        if (location == null) {
            throw new BusinessException("地点不存在：ID=" + id);
        }
        return Result.success(location);
    }

    /**
     * 获取地点的 3D 模型 URL
     */
    @GetMapping("/{id}/model-url")
    public Result<Map<String, String>> getModelUrl(@PathVariable Integer id) {
        log.info("[LocationController] 获取地点模型URL，ID={}", id);
        String modelUrl = cosService.getLocationModelUrl(id);
        Map<String, String> result = new HashMap<>();
        result.put("modelUrl", modelUrl);
        return Result.success(result);
    }

    /**
     * 获取地点的图片 URL
     */
    @GetMapping("/{id}/image-url")
    public Result<Map<String, String>> getImageUrl(@PathVariable Integer id) {
        log.info("[LocationController] 获取地点图片URL，ID={}", id);
        String imageUrl = cosService.getLocationImageUrl(id);
        Map<String, String> result = new HashMap<>();
        result.put("imageUrl", imageUrl);
        return Result.success(result);
    }
}