package org.example.xiaxiang.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.properties.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Slot（网站素材空位）管理服务
 *
 * 职责：
 * 1. 根据 application.yml 中 AppProperties 字段的填充情况，枚举所有 Slot
 * 2. 给定 SlotID，定位到 AppProperties 中对应属性并更新值
 * 3. 输出 Slot 列表（含 slotId / page / description / yamlPath / type / filled / assignedFile）
 *
 * 注意：为避免复杂的反射，此 Service 采用"枚举式映射"——每个Slot明确定义对应字段。
 *       新增素材字段时，在 buildSlotList() 里加1条即可。
 */
@Slf4j
@Service
public class SlotService {

    @Autowired
    private AppProperties appProperties;

    /** 素材类型前缀 */
    public static final String T_IMG = "IMG";
    public static final String T_AUD = "AUD";
    public static final String T_VID = "VID";
    public static final String T_MDL = "MDL";

    // ================ 基础数据 ================

    private static final String P_INDEX = "首页";
    private static final String P_CLOUD = "云游侨乡";
    private static final String P_ANATOMY = "建筑解剖";
    private static final String P_STORY = "侨乡故事";
    private static final String P_DIALECT = "方言学习";
    private static final String P_KNOWLEDGE = "知识库";
    private static final String P_CULTURE = "民俗文化";
    private static final String P_BLOG = "实践日志";
    private static final String P_VIDEO = "视频展播";
    private static final String P_TEAM = "团队成员";
    private static final String P_PHOTO = "老照片对比";
    private static final String P_QIAOPI = "侨批文化";
    private static final String P_STAMP = "虚拟盖章";

    // ================ API ================

    /**
     * 返回所有 Slot 列表
     */
    public List<SlotInfo> getAllSlots() {
        List<SlotInfo> list = buildSlotList();
        list.forEach(s -> {
            String file = readAssigned(s);
            if (file != null && !file.trim().isEmpty()) {
                s.setFilled(true);
                s.setAssignedFile(file);
            }
        });
        return list;
    }

    /**
     * 批量执行匹配写入
     * @param matches slotId -> cosKey
     * @return 成功写入的数量
     */
    public MatchResult applyMatches(Map<String, String> matches) {
        List<String> success = new ArrayList<>();
        List<String> fail = new ArrayList<>();
        Map<String, String> yamlWrites = new HashMap<>();

        List<SlotInfo> defs = buildSlotList();
        Map<String, SlotInfo> idMap = new HashMap<>();
        for (SlotInfo s : defs) idMap.put(s.getSlotId(), s);

        for (Map.Entry<String, String> e : matches.entrySet()) {
            String slotId = e.getKey();
            String cosKey = e.getValue();
            SlotInfo def = idMap.get(slotId);
            if (def == null) {
                fail.add(slotId + ": Slot不存在");
                continue;
            }
            try {
                writeToField(def, cosKey);
                yamlWrites.put(def.getYamlPath(), cosKey);
                success.add(slotId);
                log.info("[匹配写入] slotId={} yamlPath={} newKey={}", slotId, def.getYamlPath(), cosKey);
            } catch (Exception ex) {
                fail.add(slotId + ": " + ex.getMessage());
                log.error("[匹配失败] slotId={}", slotId, ex);
            }
        }

        // 同时写回 application.yml（尽量）
        int yamlWritten = 0;
        if (!yamlWrites.isEmpty()) {
            yamlWritten = YamlUpdater.patchYaml(yamlWrites);
        }

        MatchResult r = new MatchResult();
        r.setSuccess(success);
        r.setFail(fail);
        r.setYamlWrittenCount(yamlWritten);
        return r;
    }

    @Data
    public static class MatchResult {
        private List<String> success;
        private List<String> fail;
        private int yamlWrittenCount;
    }

    // ================ Slot 枚举定义 ================

    private List<SlotInfo> buildSlotList() {
        List<SlotInfo> list = new ArrayList<>();
        int n;

        // --- Module 03: buildings (建筑总览) ---
        n = safeSize(appProperties.getBuildings());
        for (int i = 0; i < n; i++) {
            final int idx = i;
            String bname = nameOf(() -> appProperties.getBuildings().get(idx).getName(), "建筑" + (idx+1));
            list.add(slot(T_IMG, 1, i+1, "buildings[" + i + "].coverImage", "项目成果/云游侨乡", bname + " · 封面图"));
            list.add(slot(T_MDL, 1, i+1, "buildings[" + i + "].modelKey", "项目成果/云游侨乡", bname + " · 3D模型"));
            list.add(slot(T_VID, 1, i+1, "buildings[" + i + "].videoKey", "项目成果/云游侨乡", bname + " · 4K视频"));
        }

        // --- Module 02: locations (云游侨乡-6个地点) ---
        n = safeSize(appProperties.getLocations());
        for (int i = 0; i < n; i++) {
            final int idx = i;
            String lname = nameOf(() -> appProperties.getLocations().get(idx).getName(), "地点" + (idx+1));
            list.add(slot(T_IMG, 2, i+1, "locations[" + i + "].imageKey", P_CLOUD, lname + " · 封面图"));
            list.add(slot(T_MDL, 2, i+1, "locations[" + i + "].modelKey", P_CLOUD, lname + " · 3D模型"));
        }

        // --- Module 04: stories (侨乡故事) ---
        n = safeSize(appProperties.getStories());
        for (int i = 0; i < n; i++) {
            final int idx = i;
            String t = nameOf(() -> appProperties.getStories().get(idx).getTitle(), "故事" + (idx+1));
            list.add(slot(T_IMG, 4, i+1, "stories[" + i + "].coverImage", P_STORY, t + " · 封面图"));
            list.add(slot(T_AUD, 4, i+1, "stories[" + i + "].audioKey", P_STORY, t + " · 朗读音频"));
        }

        // --- Module 06: knowledge (知识库) ---
        n = safeSize(appProperties.getKnowledge());
        for (int i = 0; i < n; i++) {
            final int idx = i;
            String t = nameOf(() -> appProperties.getKnowledge().get(idx).getTitle(), "知识" + (idx+1));
            list.add(slot(T_IMG, 6, i+1, "knowledge[" + i + "].coverImage", P_KNOWLEDGE, t + " · 封面图"));
        }

        // --- Module 07: cultures (民俗文化) ---
        n = safeSize(appProperties.getCultures());
        for (int i = 0; i < n; i++) {
            final int idx = i;
            String t = nameOf(() -> appProperties.getCultures().get(idx).getName(), "民俗" + (idx+1));
            list.add(slot(T_IMG, 7, i+1, "cultures[" + i + "].coverImage", P_CULTURE, t + " · 封面图"));
        }

        // --- Module 08: blogPosts (实践日志) ---
        n = safeSize(appProperties.getBlogPosts());
        for (int i = 0; i < n; i++) {
            final int idx = i;
            String t = nameOf(() -> appProperties.getBlogPosts().get(idx).getTitle(), "日志" + (idx+1));
            list.add(slot(T_IMG, 8, i+1, "blogPosts[" + i + "].coverImage", P_BLOG, t + " · 封面图"));
        }

        // --- Module 09: videos (视频展播) ---
        n = safeSize(appProperties.getVideos());
        for (int i = 0; i < n; i++) {
            final int idx = i;
            String t = nameOf(() -> appProperties.getVideos().get(idx).getTitle(), "视频" + (idx+1));
            list.add(slot(T_IMG, 9, i+1, "videos[" + i + "].coverImage", P_VIDEO, t + " · 封面图"));
            list.add(slot(T_VID, 9, i+1, "videos[" + i + "].videoKey", P_VIDEO, t + " · 视频文件"));
        }

        // --- Module 10: team (团队成员) ---
        n = safeSize(appProperties.getTeam());
        for (int i = 0; i < n; i++) {
            final int idx = i;
            String t = nameOf(() -> appProperties.getTeam().get(idx).getName(), "成员" + (idx+1));
            list.add(slot(T_IMG, 10, i+1, "team[" + i + "].avatar", P_TEAM, t + " · 头像"));
        }

        // --- Module 05: anatomies (建筑解剖) ---
        n = safeSize(appProperties.getAnatomies());
        for (int i = 0; i < n; i++) {
            final int idx = i;
            String t = nameOf(() -> appProperties.getAnatomies().get(idx).getPartName(), "部位" + (idx+1));
            list.add(slot(T_IMG, 5, i+1, "anatomies[" + i + "].imageKey", P_ANATOMY, t + " · 图片"));
            list.add(slot(T_MDL, 5, i+1, "anatomies[" + i + "].modelKey", P_ANATOMY, t + " · 分体模型"));
        }

        // --- Module 11: photoCompares (老照片对比) ---
        n = safeSize(appProperties.getPhotoCompares());
        for (int i = 0; i < n; i++) {
            final int idx = i;
            list.add(slot(T_IMG, 11, (i*2+1), "photoCompares[" + i + "].oldImageKey", P_PHOTO, "对比组" + (idx+1) + " · 老照片"));
            list.add(slot(T_IMG, 11, (i*2+2), "photoCompares[" + i + "].newImageKey", P_PHOTO, "对比组" + (idx+1) + " · 新照片"));
        }

        // --- Module 12: qiaopi (侨批文化) ---
        n = safeSize(appProperties.getQiaopi());
        for (int i = 0; i < n; i++) {
            final int idx = i;
            list.add(slot(T_IMG, 12, i+1, "qiaopi[" + i + "].imageKey", P_QIAOPI, "侨批" + (idx+1) + " · 扫描件"));
        }

        // --- Module 13: dialects (方言学习) ---
        n = safeSize(appProperties.getDialects());
        for (int i = 0; i < n; i++) {
            final int idx = i;
            String t = nameOf(() -> appProperties.getDialects().get(idx).getChinese(), "方言" + (idx+1));
            list.add(slot(T_AUD, 13, i+1, "dialects[" + i + "].audioKey", P_DIALECT, t + " · 录音"));
        }

        // --- Module 15: stamps (虚拟盖章) ---
        n = safeSize(appProperties.getStamps());
        for (int i = 0; i < n; i++) {
            final int idx = i;
            String t = nameOf(() -> appProperties.getStamps().get(idx).getName(), "印章" + (idx+1));
            list.add(slot(T_IMG, 15, i+1, "stamps[" + i + "].imageKey", P_STAMP, t + " · 图标"));
        }

        return list;
    }

    private SlotInfo slot(String typePrefix, int moduleIndex, int posIndex, String yamlPath, String page, String description) {
        SlotInfo s = new SlotInfo();
        s.setSlotId(String.format("%s-%02d-%02d", typePrefix, moduleIndex, posIndex));
        s.setYamlPath(yamlPath);
        s.setPage(page);
        s.setDescription(description);
        s.setFilled(false);
        s.setAssignedFile(null);
        return s;
    }

    // ================ 字段读写（通过 AppProperties getter/setter 反射友好替换）================

    /**
     * 从 AppProperties 实例按 yamlPath 读取填充值。
     * yamlPath 形如：locations[2].imageKey  / buildings[0].coverImage
     */
    private String readAssigned(SlotInfo s) {
        try {
            return FieldAccessor.readField(appProperties, s.getYamlPath());
        } catch (Exception e) {
            log.warn("读取Slot字段失败: {} err={}", s.getYamlPath(), e.getMessage());
            return null;
        }
    }

    private void writeToField(SlotInfo s, String cosKey) throws Exception {
        FieldAccessor.writeField(appProperties, s.getYamlPath(), cosKey);
    }

    // ================ 工具 ================

    @FunctionalInterface
    private interface StrGetter { String get(); }

    private static String nameOf(StrGetter g, String fallback) {
        try {
            String v = g.get();
            return (v == null || v.isEmpty()) ? fallback : v;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int safeSize(List<?> l) { return l == null ? 0 : l.size(); }

    // ================ Slot 信息 POJO ================

    @Data
    public static class SlotInfo {
        private String slotId;
        private String page;
        private String description;
        private String yamlPath;
        private boolean filled;
        private String assignedFile;
    }
}
