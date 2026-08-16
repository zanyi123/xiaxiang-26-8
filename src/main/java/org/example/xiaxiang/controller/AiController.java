package org.example.xiaxiang.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.example.xiaxiang.properties.AppProperties;
import org.example.xiaxiang.service.CosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 讲解控制器
 *
 * 设计理念：
 *   后端只负责"返回讲解文本"，实际音频播放由前端 QiaoyunTTS 工具完成
 *   （浏览器原生 speechSynthesis，无需云 TTS 服务/无需 API Key/无需计费）
 *
 * 支持的讲解类型：
 *   - location : 宝源坊地点讲解（优先 audioText，回退 description）
 *   - building : 建筑讲解
 *   - story    : 侨乡故事讲解
 *   - anatomy  : 建筑部位解剖讲解（含功能/材料/年代）
 *   - dialect  : 方言词条讲解
 *   - knowledge: 知识库讲解
 *   - culture  : 民俗文化讲解
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private CosService cosService;

    @Autowired
    private AppProperties appProperties;

    /**
     * 统一获取讲解文本接口
     *
     * @param type 讲解类型（location/building/story/anatomy/dialect/knowledge/culture）
     * @param id   对象 ID
     * @return { audioText, title, source }
     */
    @GetMapping("/guide")
    public Result<Map<String, String>> getGuide(
            @RequestParam String type,
            @RequestParam Integer id) {
        log.info("[AiController] 获取讲解文本，type={}, id={}", type, id);
        Map<String, String> result = buildGuideText(type, id);
        if (result == null) {
            return Result.fail("不支持的讲解类型：" + type);
        }
        return Result.success(result);
    }

    /**
     * 兼容旧接口：按 locationId 获取讲解文本
     * @deprecated 请使用 GET /api/ai/guide?type=location&id={locationId}
     */
    @Deprecated
    @GetMapping("/guide/{locationId}")
    public Result<Map<String, String>> getGuideText(@PathVariable Integer locationId) {
        log.info("[AiController] （兼容旧接口）获取地点讲解文本，locationId={}", locationId);
        Map<String, String> result = buildGuideText("location", locationId);
        if (result == null) {
            return Result.fail("地点不存在：ID=" + locationId);
        }
        return Result.success(result);
    }

    /**
     * 根据类型构建讲解文本
     *
     * @return null 表示类型不支持或对象不存在
     */
    private Map<String, String> buildGuideText(String type, Integer id) {
        if (type == null || id == null) return null;
        type = type.trim().toLowerCase();

        Map<String, String> result = new HashMap<>();
        result.put("source", type);

        switch (type) {
            case "location": {
                AppProperties.Location obj = cosService.findLocationById(id);
                if (obj == null) return null;
                result.put("title", obj.getName());
                // 优先 audioText，回退 description
                String text = obj.getAudioText();
                if (text == null || text.trim().isEmpty()) {
                    text = obj.getDescription();
                }
                result.put("audioText", safe(text));
                return result;
            }
            case "building": {
                AppProperties.Building obj = findBuildingById(id);
                if (obj == null) return null;
                result.put("title", obj.getName());
                // 优先 audioText，回退 description
                String text = obj.getAudioText();
                if (text == null || text.trim().isEmpty()) {
                    text = obj.getDescription();
                }
                result.put("audioText", safe(text));
                return result;
            }
            case "story": {
                AppProperties.Story obj = findStoryById(id);
                if (obj == null) return null;
                result.put("title", obj.getTitle());
                // 优先 audioText，回退 content，再回退 summary
                String text = obj.getAudioText();
                if (text == null || text.trim().isEmpty()) {
                    text = obj.getContent();
                    if (text == null || text.trim().isEmpty()) {
                        text = obj.getSummary();
                    }
                }
                result.put("audioText", safe(text));
                return result;
            }
            case "anatomy": {
                AppProperties.BuildingAnatomy obj = findAnatomyById(id);
                if (obj == null) return null;
                result.put("title", obj.getPartName());
                // 优先 audioText，回退到拼装文本
                String text = obj.getAudioText();
                if (text == null || text.trim().isEmpty()) {
                    // 解剖讲解：拼装 部位名 + 描述 + 功能 + 材料 + 年代
                    StringBuilder sb = new StringBuilder();
                    sb.append(safe(obj.getPartName())).append("。");
                    sb.append(safe(obj.getDescription())).append("。");
                    if (obj.getFunction() != null && !obj.getFunction().trim().isEmpty()) {
                        sb.append("它的主要功能是").append(obj.getFunction()).append("。");
                    }
                    if (obj.getMaterial() != null && !obj.getMaterial().trim().isEmpty()) {
                        sb.append("采用").append(obj.getMaterial()).append("建造。");
                    }
                    if (obj.getEra() != null && !obj.getEra().trim().isEmpty()) {
                        sb.append("建造年代约为").append(obj.getEra()).append("。");
                    }
                    text = sb.toString();
                }
                result.put("audioText", safe(text));
                return result;
            }
            case "dialect": {
                AppProperties.DialectItem obj = findDialectById(id);
                if (obj == null) return null;
                result.put("title", obj.getDialect());
                // 优先 audioText，回退到拼装文本
                String text = obj.getAudioText();
                if (text == null || text.trim().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(safe(obj.getDialect())).append("。");
                    if (obj.getChinese() != null) sb.append(obj.getChinese()).append("。");
                    if (obj.getMeaning() != null) sb.append("意思是，").append(obj.getMeaning()).append("。");
                    if (obj.getExample() != null) sb.append("例句：").append(obj.getExample()).append("。");
                    text = sb.toString();
                }
                result.put("audioText", safe(text));
                return result;
            }
            case "knowledge": {
                AppProperties.KnowledgeItem obj = findKnowledgeById(id);
                if (obj == null) return null;
                result.put("title", obj.getTitle());
                // 优先 audioText，回退 content，再回退 summary
                String text = obj.getAudioText();
                if (text == null || text.trim().isEmpty()) {
                    text = obj.getContent();
                    if (text == null || text.trim().isEmpty()) {
                        text = obj.getSummary();
                    }
                }
                result.put("audioText", safe(text));
                return result;
            }
            case "culture": {
                AppProperties.CultureItem obj = findCultureById(id);
                if (obj == null) return null;
                result.put("title", obj.getName());
                // 优先 audioText，回退 description
                String text = obj.getAudioText();
                if (text == null || text.trim().isEmpty()) {
                    text = obj.getDescription();
                }
                result.put("audioText", safe(text));
                return result;
            }
            default:
                return null;
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    // ====== 实体查找（轻量内联，避免引入多个 Controller 依赖） ======

    private AppProperties.Building findBuildingById(Integer id) {
        if (appProperties.getBuildings() == null) return null;
        return appProperties.getBuildings().stream()
                .filter(b -> id.equals(b.getId()))
                .findFirst().orElse(null);
    }

    private AppProperties.Story findStoryById(Integer id) {
        if (appProperties.getStories() == null) return null;
        return appProperties.getStories().stream()
                .filter(s -> id.equals(s.getId()))
                .findFirst().orElse(null);
    }

    private AppProperties.BuildingAnatomy findAnatomyById(Integer id) {
        if (appProperties.getAnatomies() == null) return null;
        return appProperties.getAnatomies().stream()
                .filter(a -> id.equals(a.getId()))
                .findFirst().orElse(null);
    }

    private AppProperties.DialectItem findDialectById(Integer id) {
        if (appProperties.getDialects() == null) return null;
        return appProperties.getDialects().stream()
                .filter(d -> id.equals(d.getId()))
                .findFirst().orElse(null);
    }

    private AppProperties.KnowledgeItem findKnowledgeById(Integer id) {
        if (appProperties.getKnowledge() == null) return null;
        return appProperties.getKnowledge().stream()
                .filter(k -> id.equals(k.getId()))
                .findFirst().orElse(null);
    }

    private AppProperties.CultureItem findCultureById(Integer id) {
        if (appProperties.getCultures() == null) return null;
        return appProperties.getCultures().stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst().orElse(null);
    }
}
