package org.example.xiaxiang.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.example.xiaxiang.service.CosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI讲解控制器
 *
 * 提供：获取讲解内容、播放/暂停讲解等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private CosService cosService;

    /**
     * 获取指定地点的AI讲解文本
     */
    @GetMapping("/guide/{locationId}")
    public Result<Map<String, String>> getGuideText(@PathVariable Integer locationId) {
        log.info("[AiController] 获取AI讲解文本，地点ID={}", locationId);
        String audioText = cosService.getLocationAudioText(locationId);
        Map<String, String> result = new HashMap<>();
        result.put("audioText", audioText);
        return Result.success(result);
    }

    /**
     * 播放指定地点的AI讲解
     */
    @PostMapping("/guide/{locationId}/play")
    public Result<Map<String, Object>> playGuide(@PathVariable Integer locationId) {
        log.info("[AiController] 播放AI讲解，地点ID={}", locationId);
        String audioText = cosService.getLocationAudioText(locationId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("locationId", locationId);
        result.put("audioText", audioText);
        result.put("status", "playing");
        result.put("message", "讲解已开始");
        
        return Result.success(result);
    }

    /**
     * 暂停当前讲解
     */
    @PostMapping("/guide/pause")
    public Result<Map<String, String>> pauseGuide() {
        log.info("[AiController] 暂停AI讲解");
        Map<String, String> result = new HashMap<>();
        result.put("status", "paused");
        result.put("message", "讲解已暂停");
        return Result.success(result);
    }

    /**
     * 继续当前讲解
     */
    @PostMapping("/guide/resume")
    public Result<Map<String, String>> resumeGuide() {
        log.info("[AiController] 继续AI讲解");
        Map<String, String> result = new HashMap<>();
        result.put("status", "playing");
        result.put("message", "讲解已继续");
        return Result.success(result);
    }
}