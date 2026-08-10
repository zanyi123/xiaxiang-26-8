package org.example.xiaxiang.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.properties.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 内容管理服务
 *
 * 职责：
 * 1. 枚举所有可管理的专栏模块（对应 application.yml 中的列表）
 * 2. 对各模块的单元执行新增/编辑/删除操作
 * 3. 新增单元时自动分配 ID 和素材 Slot 编号
 * 4. 操作同步写入 application.yml（通过 YamlInserter）
 *
 * 设计原则：
 * - 内存优先：先操作 AppProperties 的 List（立即生效），再尝试写盘
 * - 单元框架保留：新增单元时只填操作者提供的字段，其余留空
 */
@Slf4j
@Service
public class ContentManageService {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private CosService cosService;

    @Autowired
    private SlotService slotService;

    // ================ 模块定义 ================

    /**
     * 可管理的专栏模块定义
     */
    @Data
    public static class ModuleDef {
        private String key;           // 模块标识（如 qiaopi）
        private String name;          // 显示名称（如 侨批文化）
        private int moduleIndex;      // Slot 模块编号（如 12）
        private String slotPrefix;    // 素材前缀（如 IMG）
        private List<FieldDef> fields; // 可编辑字段
    }

    @Data
    public static class FieldDef {
        private String name;        // 字段名（Java 属性名，如 title）
        private String label;       // 显示标签（如 标题）
        private String type;        // text / textarea / number / select
        private boolean required;   // 是否必填
        private List<String> options; // select 类型的选项
    }

    /**
     * 获取所有可管理的模块定义
     */
    public List<ModuleDef> getModuleDefs() {
        List<ModuleDef> list = new ArrayList<>();
        list.add(buildQiaopiDef());
        list.add(buildStoryDef());
        list.add(buildKnowledgeDef());
        list.add(buildDialectDef());
        list.add(buildCultureDef());
        list.add(buildBlogDef());
        list.add(buildVideoDef());
        list.add(buildAnatomyDef());
        list.add(buildPhotoCompareDef());
        list.add(buildTeamDef());
        list.add(buildArchiveDef());
        list.add(buildLocationDef());
        return list;
    }

    // ================ 各模块字段定义 ================

    private ModuleDef buildQiaopiDef() {
        ModuleDef m = new ModuleDef();
        m.setKey("qiaopi"); m.setName("侨批文化"); m.setModuleIndex(12); m.setSlotPrefix("IMG");
        m.setFields(Arrays.asList(
            field("title", "标题", "text", true, null),
            field("sender", "寄信人", "text", false, null),
            field("recipient", "收信人", "text", false, null),
            field("sendFrom", "寄出地", "text", false, null),
            field("year", "年份", "text", false, null),
            field("amount", "金额", "text", false, null),
            field("content", "书信内容", "textarea", false, null),
            field("translation", "英文翻译", "textarea", false, null),
            field("category", "分类", "select", false, Arrays.asList("亲情类", "商务类", "其他"))
        ));
        return m;
    }

    private ModuleDef buildStoryDef() {
        ModuleDef m = new ModuleDef();
        m.setKey("stories"); m.setName("侨乡故事"); m.setModuleIndex(4); m.setSlotPrefix("IMG");
        m.setFields(Arrays.asList(
            field("title", "标题", "text", true, null),
            field("category", "分类", "select", false, Arrays.asList("建筑故事", "口述历史", "侨批文化", "教育故事", "民间传说")),
            field("summary", "摘要", "textarea", false, null),
            field("content", "正文", "textarea", false, null),
            field("author", "作者", "text", false, null)
        ));
        return m;
    }

    private ModuleDef buildKnowledgeDef() {
        ModuleDef m = new ModuleDef();
        m.setKey("knowledge"); m.setName("知识库"); m.setModuleIndex(6); m.setSlotPrefix("IMG");
        m.setFields(Arrays.asList(
            field("title", "标题", "text", true, null),
            field("category", "分类", "select", false, Arrays.asList("建筑知识", "建筑技术", "历史文化")),
            field("summary", "摘要", "textarea", false, null),
            field("content", "正文", "textarea", false, null),
            field("difficulty", "难度", "select", false, Arrays.asList("入门", "进阶", "高级"))
        ));
        return m;
    }

    private ModuleDef buildDialectDef() {
        ModuleDef m = new ModuleDef();
        m.setKey("dialects"); m.setName("方言学习"); m.setModuleIndex(13); m.setSlotPrefix("AUD");
        m.setFields(Arrays.asList(
            field("chinese", "中文字", "text", true, null),
            field("dialect", "方言发音", "text", false, null),
            field("pinyin", "拼音", "text", false, null),
            field("meaning", "释义", "text", false, null),
            field("example", "例句", "textarea", false, null),
            field("category", "分类", "select", false, Arrays.asList("日常用语", "礼貌用语", "建筑相关", "其他"))
        ));
        return m;
    }

    private ModuleDef buildCultureDef() {
        ModuleDef m = new ModuleDef();
        m.setKey("cultures"); m.setName("民俗文化"); m.setModuleIndex(7); m.setSlotPrefix("IMG");
        m.setFields(Arrays.asList(
            field("name", "名称", "text", true, null),
            field("category", "分类", "select", false, Arrays.asList("民俗活动", "饮食文化", "传统技艺", "其他")),
            field("description", "描述", "textarea", false, null)
        ));
        return m;
    }

    private ModuleDef buildBlogDef() {
        ModuleDef m = new ModuleDef();
        m.setKey("blogPosts"); m.setName("实践日志"); m.setModuleIndex(8); m.setSlotPrefix("IMG");
        m.setFields(Arrays.asList(
            field("title", "标题", "text", true, null),
            field("summary", "摘要", "textarea", false, null),
            field("author", "作者", "text", false, null),
            field("category", "分类", "select", false, Arrays.asList("田野调查", "技术研究", "团队日志", "其他"))
        ));
        return m;
    }

    private ModuleDef buildVideoDef() {
        ModuleDef m = new ModuleDef();
        m.setKey("videos"); m.setName("视频展播"); m.setModuleIndex(9); m.setSlotPrefix("IMG");
        m.setFields(Arrays.asList(
            field("title", "标题", "text", true, null),
            field("description", "描述", "textarea", false, null),
            field("duration", "时长", "text", false, null)
        ));
        return m;
    }

    private ModuleDef buildAnatomyDef() {
        ModuleDef m = new ModuleDef();
        m.setKey("anatomies"); m.setName("建筑解剖"); m.setModuleIndex(5); m.setSlotPrefix("IMG");
        m.setFields(Arrays.asList(
            field("partName", "部位名称", "text", true, null),
            field("partNameEn", "英文名称", "text", false, null),
            field("category", "分类", "select", false, Arrays.asList("主体结构", "防御系统", "装饰元素", "其他")),
            field("description", "描述", "textarea", false, null),
            field("function", "功能", "text", false, null),
            field("material", "材料", "text", false, null),
            field("era", "年代", "text", false, null)
        ));
        return m;
    }

    private ModuleDef buildPhotoCompareDef() {
        ModuleDef m = new ModuleDef();
        m.setKey("photoCompares"); m.setName("老照片对比"); m.setModuleIndex(11); m.setSlotPrefix("IMG");
        m.setFields(Arrays.asList(
            field("title", "标题", "text", true, null),
            field("location", "地点", "text", false, null),
            field("yearOld", "旧照年份", "text", false, null),
            field("yearNew", "新照年份", "text", false, null),
            field("description", "描述", "textarea", false, null),
            field("story", "故事", "textarea", false, null)
        ));
        return m;
    }

    private ModuleDef buildTeamDef() {
        ModuleDef m = new ModuleDef();
        m.setKey("team"); m.setName("团队成员"); m.setModuleIndex(10); m.setSlotPrefix("IMG");
        m.setFields(Arrays.asList(
            field("name", "姓名", "text", true, null),
            field("role", "角色", "text", false, null),
            field("major", "专业", "text", false, null),
            field("bio", "简介", "textarea", false, null)
        ));
        return m;
    }

    private ModuleDef buildLocationDef() {
        ModuleDef m = new ModuleDef();
        m.setKey("locations"); m.setName("建筑故事"); m.setModuleIndex(2); m.setSlotPrefix("IMG");
        m.setFields(Arrays.asList(
            field("name", "建筑名称", "text", true, null),
            field("number", "编号", "text", false, null),
            field("description", "简短描述", "textarea", false, null),
            field("history", "历史背景", "textarea", false, null),
            field("audioText", "AI讲解文案", "textarea", false, null),
            field("xCoordinate", "地图X坐标", "text", false, null),
            field("yCoordinate", "地图Y坐标", "text", false, null)
        ));
        return m;
    }

    private ModuleDef buildArchiveDef() {
        ModuleDef m = new ModuleDef();
        m.setKey("archives"); m.setName("田野档案"); m.setModuleIndex(0); m.setSlotPrefix("IMG");
        m.setFields(Arrays.asList(
            field("title", "标题", "text", true, null),
            field("category", "分类", "select", false, Arrays.asList("文献档案", "影像档案", "家族档案", "其他")),
            field("description", "描述", "textarea", false, null),
            field("icon", "图标", "text", false, null),
            field("unit", "计量单位", "text", false, null),
            field("detailUrl", "详情URL", "text", false, null)
        ));
        return m;
    }

    private FieldDef field(String name, String label, String type, boolean required, List<String> options) {
        FieldDef f = new FieldDef();
        f.setName(name); f.setLabel(label); f.setType(type); f.setRequired(required); f.setOptions(options);
        return f;
    }

    // ================ CRUD 操作 ================

    /**
     * 获取模块下所有单元（含素材编号信息）
     * 返回 List<Map>，每个 Map 包含单元字段 + slotIds + slotFilled
     */
    public List<Map<String, Object>> listItemsWithSlots(String moduleKey) {
        List<?> items = getList(moduleKey);
        ModuleDef def = findDef(moduleKey);
        if (def == null) {
            throw new IllegalArgumentException("未知模块: " + moduleKey);
        }

        // 从 SlotService 获取当前所有 Slot 状态
        List<org.example.xiaxiang.service.SlotService.SlotInfo> allSlots = slotService.getAllSlots();

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> itemMap = new LinkedHashMap<>();
            Object item = items.get(i);

            // 通过反射提取所有字段
            try {
                for (java.lang.reflect.Field f : item.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    Object val = f.get(item);
                    itemMap.put(f.getName(), val);
                }
            } catch (Exception e) {
                log.warn("[ContentManage] 反射读取字段失败: {}", e.getMessage());
            }

            // 生成该单元的 Slot 编号
            List<String> slotIds = generateSlotIds(def, i + 1);
            itemMap.put("slotIds", slotIds);

            // 检查每个 Slot 的填充状态
            List<Map<String, Object>> slotDetails = new ArrayList<>();
            for (String slotId : slotIds) {
                Map<String, Object> slotDetail = new LinkedHashMap<>();
                slotDetail.put("slotId", slotId);
                // 从 allSlots 中查找匹配的 slot
                org.example.xiaxiang.service.SlotService.SlotInfo match = null;
                for (org.example.xiaxiang.service.SlotService.SlotInfo s : allSlots) {
                    if (s.getSlotId().equals(slotId)) {
                        match = s;
                        break;
                    }
                }
                if (match != null) {
                    slotDetail.put("filled", match.isFilled());
                    slotDetail.put("assignedFile", match.getAssignedFile());
                } else {
                    slotDetail.put("filled", false);
                    slotDetail.put("assignedFile", null);
                }
                slotDetails.add(slotDetail);
            }
            itemMap.put("slotDetails", slotDetails);

            result.add(itemMap);
        }
        return result;
    }

    /**
     * 获取模块下所有单元（原始对象，不含 Slot 信息）
     */
    public List<?> listItems(String moduleKey) {
        return getList(moduleKey);
    }

    /**
     * 素材编号校对：返回模块下所有单元与 Slot 的对应关系
     */
    public Map<String, Object> verifySlots(String moduleKey) {
        ModuleDef def = findDef(moduleKey);
        if (def == null) {
            throw new IllegalArgumentException("未知模块: " + moduleKey);
        }

        List<?> items = getList(moduleKey);
        List<org.example.xiaxiang.service.SlotService.SlotInfo> allSlots = slotService.getAllSlots();

        // 筛选出属于该模块的 Slot
        String modulePrefix = String.format("-%02d-", def.getModuleIndex());
        List<Map<String, Object>> slotList = new ArrayList<>();
        int filledCount = 0;
        int emptyCount = 0;

        for (org.example.xiaxiang.service.SlotService.SlotInfo s : allSlots) {
            if (s.getSlotId().contains(modulePrefix)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("slotId", s.getSlotId());
                m.put("page", s.getPage());
                m.put("description", s.getDescription());
                m.put("yamlPath", s.getYamlPath());
                m.put("filled", s.isFilled());
                m.put("assignedFile", s.getAssignedFile());
                if (s.isFilled()) {
                    filledCount++;
                } else {
                    emptyCount++;
                }
                slotList.add(m);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("module", def.getName());
        result.put("moduleKey", moduleKey);
        result.put("unitCount", items.size());
        result.put("slotCount", slotList.size());
        result.put("filledCount", filledCount);
        result.put("emptyCount", emptyCount);
        result.put("slots", slotList);
        return result;
    }

    /**
     * 全模块素材编号校对
     */
    public List<Map<String, Object>> verifyAllSlots() {
        List<Map<String, Object>> results = new ArrayList<>();
        for (ModuleDef def : getModuleDefs()) {
            results.add(verifySlots(def.getKey()));
        }
        return results;
    }

    /**
     * 删除前检查：返回该单元绑定的素材信息
     */
    public Map<String, Object> checkBeforeDelete(String moduleKey, int id) {
        ModuleDef def = findDef(moduleKey);
        if (def == null) {
            throw new IllegalArgumentException("未知模块: " + moduleKey);
        }

        List<Object> list = getList(moduleKey);
        int index = findIndexById(list, id);
        if (index < 0) {
            throw new IllegalArgumentException("单元不存在: id=" + id);
        }

        // 生成该单元的 Slot 编号
        List<String> slotIds = generateSlotIds(def, index + 1);

        // 从 SlotService 获取填充状态
        List<org.example.xiaxiang.service.SlotService.SlotInfo> allSlots = slotService.getAllSlots();
        List<Map<String, Object>> boundSlots = new ArrayList<>();
        boolean hasBound = false;

        for (String slotId : slotIds) {
            for (org.example.xiaxiang.service.SlotService.SlotInfo s : allSlots) {
                if (s.getSlotId().equals(slotId) && s.isFilled()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("slotId", slotId);
                    m.put("assignedFile", s.getAssignedFile());
                    m.put("yamlPath", s.getYamlPath());
                    boundSlots.add(m);
                    hasBound = true;
                    break;
                }
            }
        }

        // 检查删除后是否会导致后续单元编号错位
        boolean willShift = index < list.size() - 1;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("module", def.getName());
        result.put("hasBoundMaterials", hasBound);
        result.put("boundSlots", boundSlots);
        result.put("willShiftIndex", willShift);
        result.put("warning", hasBound
                ? "该单元已绑定 " + boundSlots.size() + " 个素材文件，删除后绑定关系将丢失"
                : (willShift ? "删除后后续单元的素材编号将错位，需重新绑定" : "安全删除，无素材绑定"));
        return result;
    }

    /**
     * 新增单元
     * @return 新单元的 ID 和分配的 Slot 编号
     */
    public Map<String, Object> createItem(String moduleKey, Map<String, String> formData) {
        ModuleDef def = findDef(moduleKey);
        if (def == null) {
            throw new IllegalArgumentException("未知模块: " + moduleKey);
        }

        List<Object> list = getList(moduleKey);
        int newId = nextId(list);
        int newIndex = list.size();

        // 判断是否需要生成素材模板
        boolean hasMaterials = !"false".equals(formData.get("hasMaterials"));

        // 创建新对象并填充字段
        Object newItem;
        try {
            newItem = createAndPopulate(moduleKey, newId, formData);
            // 设置 hasMaterials 标志
            try { setField(newItem, "hasMaterials", String.valueOf(hasMaterials)); } catch (Exception ignored) {}
        } catch (Exception e) {
            throw new RuntimeException("创建对象失败: " + moduleKey, e);
        }

        // 添加到内存列表
        list.add(newItem);

        // 写入 YAML（在列表末尾追加新条目）
        String yamlBlock = buildYamlBlock(moduleKey, newId, formData);
        int written = YamlInserter.appendListItem(yamlBlock, moduleKey);

        // 清除缓存
        cosService.clearCache();

        // 生成 Slot 编号（仅当需要素材时）
        List<String> slotIds = hasMaterials ? generateSlotIds(def, newIndex + 1) : new ArrayList<>();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", newId);
        result.put("index", newIndex);
        result.put("hasMaterials", hasMaterials);
        result.put("slotIds", slotIds);
        result.put("yamlWritten", written > 0);
        log.info("[ContentManage] 新增 {} 单元: id={}, hasMaterials={}, slots={}", moduleKey, newId, hasMaterials, slotIds);
        return result;
    }

    /**
     * 编辑单元
     */
    public Map<String, Object> updateItem(String moduleKey, int id, Map<String, String> formData) {
        ModuleDef def = findDef(moduleKey);
        if (def == null) {
            throw new IllegalArgumentException("未知模块: " + moduleKey);
        }

        List<Object> list = getList(moduleKey);
        int index = findIndexById(list, id);
        if (index < 0) {
            throw new IllegalArgumentException("单元不存在: id=" + id);
        }

        // 更新内存对象字段
        Object item = list.get(index);
        for (FieldDef f : def.getFields()) {
            String val = formData.get(f.getName());
            if (val != null) {
                try {
                    setField(item, f.getName(), val);
                } catch (Exception e) {
                    log.warn("[ContentManage] 更新字段失败 {}.{}, err={}", moduleKey, f.getName(), e.getMessage());
                }
            }
        }

        // 写入 YAML
        Map<String, String> yamlWrites = new LinkedHashMap<>();
        for (FieldDef f : def.getFields()) {
            String val = formData.get(f.getName());
            if (val != null) {
                yamlWrites.put(moduleKey + "[" + index + "]." + f.getName(), val);
            }
        }
        int written = YamlUpdater.patchYaml(yamlWrites);
        cosService.clearCache();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("yamlWritten", written);
        log.info("[ContentManage] 更新 {} 单元: id={}", moduleKey, id);
        return result;
    }

    /**
     * 删除单元
     */
    public Map<String, Object> deleteItem(String moduleKey, int id) {
        List<Object> list = getList(moduleKey);
        int index = findIndexById(list, id);
        if (index < 0) {
            throw new IllegalArgumentException("单元不存在: id=" + id);
        }

        // 从内存列表移除
        list.remove(index);

        // YAML 删除（重建整个列表块）
        int written = YamlInserter.removeListItem(moduleKey, index);

        cosService.clearCache();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("yamlWritten", written > 0);
        log.info("[ContentManage] 删除 {} 单元: id={}, index={}", moduleKey, id, index);
        return result;
    }

    // ================ 工具方法 ================

    @SuppressWarnings("unchecked")
    private List<Object> getList(String moduleKey) {
        switch (moduleKey) {
            case "qiaopi": return (List<Object>) (List<?>) appProperties.getQiaopi();
            case "stories": return (List<Object>) (List<?>) appProperties.getStories();
            case "knowledge": return (List<Object>) (List<?>) appProperties.getKnowledge();
            case "dialects": return (List<Object>) (List<?>) appProperties.getDialects();
            case "cultures": return (List<Object>) (List<?>) appProperties.getCultures();
            case "blogPosts": return (List<Object>) (List<?>) appProperties.getBlogPosts();
            case "videos": return (List<Object>) (List<?>) appProperties.getVideos();
            case "anatomies": return (List<Object>) (List<?>) appProperties.getAnatomies();
            case "photoCompares": return (List<Object>) (List<?>) appProperties.getPhotoCompares();
            case "team": return (List<Object>) (List<?>) appProperties.getTeam();
            case "archives": return (List<Object>) (List<?>) appProperties.getArchives();
            case "locations": return (List<Object>) (List<?>) appProperties.getLocations();
            default: throw new IllegalArgumentException("未知模块: " + moduleKey);
        }
    }

    private Object createAndPopulate(String moduleKey, int id, Map<String, String> formData) throws Exception {
        Object item;
        try {
            switch (moduleKey) {
                case "qiaopi":
                    item = AppProperties.QiaopiItem.class.getDeclaredConstructor().newInstance();
                    break;
                case "stories":
                    item = AppProperties.Story.class.getDeclaredConstructor().newInstance();
                    break;
                case "knowledge":
                    item = AppProperties.KnowledgeItem.class.getDeclaredConstructor().newInstance();
                    break;
                case "dialects":
                    item = AppProperties.DialectItem.class.getDeclaredConstructor().newInstance();
                    break;
                case "cultures":
                    item = AppProperties.CultureItem.class.getDeclaredConstructor().newInstance();
                    break;
                case "blogPosts":
                    item = AppProperties.BlogPost.class.getDeclaredConstructor().newInstance();
                    break;
                case "videos":
                    item = AppProperties.VideoItem.class.getDeclaredConstructor().newInstance();
                    break;
                case "anatomies":
                    item = AppProperties.BuildingAnatomy.class.getDeclaredConstructor().newInstance();
                    break;
                case "photoCompares":
                    item = AppProperties.PhotoCompare.class.getDeclaredConstructor().newInstance();
                    break;
                case "team":
                    item = AppProperties.TeamMember.class.getDeclaredConstructor().newInstance();
                    break;
                case "archives":
                    item = AppProperties.ArchiveItem.class.getDeclaredConstructor().newInstance();
                    break;
                case "locations":
                    item = AppProperties.Location.class.getDeclaredConstructor().newInstance();
                    break;
                default:
                    throw new IllegalArgumentException("未知模块: " + moduleKey);
            }
        } catch (Exception e) {
            throw new RuntimeException("创建对象失败: " + moduleKey, e);
        }

        // 设置 ID
        setField(item, "id", String.valueOf(id));

        // 初始化 views 为 0（story/video 有此字段，类型为 Integer）
        try { setField(item, "views", "0"); } catch (Exception ignored) {}
        // 初始化 count 为 0（archive 有此字段，类型为 String）
        try { setField(item, "count", "0"); } catch (Exception ignored) {}

        // 填充表单字段
        ModuleDef def = findDef(moduleKey);
        if (def != null) {
            for (FieldDef f : def.getFields()) {
                String val = formData.get(f.getName());
                if (val != null && !val.isEmpty()) {
                    try {
                        setField(item, f.getName(), val);
                    } catch (Exception e) {
                        log.warn("[ContentManage] 设置字段失败 {}.{}, err={}", moduleKey, f.getName(), e.getMessage());
                    }
                }
            }
        }

        // blogPosts 自动填充 date 为空（后续由操作者编辑）
        return item;
    }

    private void setField(Object target, String fieldName, String value) throws Exception {
        Class<?> c = target.getClass();
        String setter = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        for (Method m : c.getMethods()) {
            if (m.getName().equals(setter) && m.getParameterCount() == 1) {
                Class<?> pt = m.getParameterTypes()[0];
                Object casted;
                if (pt == String.class) {
                    casted = value;
                } else if (pt == Integer.class || pt == int.class) {
                    casted = Integer.parseInt(value);
                } else if (pt == Double.class || pt == double.class) {
                    casted = Double.parseDouble(value);
                } else {
                    casted = value;
                }
                m.invoke(target, casted);
                return;
            }
        }
        // 尝试反射字段直接写入
        try {
            java.lang.reflect.Field f = c.getDeclaredField(fieldName);
            f.setAccessible(true);
            if (f.getType() == String.class) {
                f.set(target, value);
            } else if (f.getType() == int.class || f.getType() == Integer.class) {
                f.set(target, Integer.parseInt(value));
            }
        } catch (NoSuchFieldException e) {
            log.debug("[ContentManage] 字段不存在: {}.{}", c.getSimpleName(), fieldName);
        }
    }

    private int nextId(List<?> list) {
        int max = 0;
        for (Object item : list) {
            try {
                Object idVal = item.getClass().getMethod("getId").invoke(item);
                if (idVal instanceof Integer) {
                    max = Math.max(max, (Integer) idVal);
                }
            } catch (Exception ignored) {}
        }
        return max + 1;
    }

    private int findIndexById(List<?> list, int id) {
        for (int i = 0; i < list.size(); i++) {
            try {
                Object idVal = list.get(i).getClass().getMethod("getId").invoke(list.get(i));
                if (idVal instanceof Integer && (Integer) idVal == id) {
                    return i;
                }
            } catch (Exception ignored) {}
        }
        return -1;
    }

    private ModuleDef findDef(String moduleKey) {
        for (ModuleDef d : getModuleDefs()) {
            if (d.getKey().equals(moduleKey)) return d;
        }
        return null;
    }

    private List<String> generateSlotIds(ModuleDef def, int posIndex) {
        List<String> slots = new ArrayList<>();
        String prefix = def.getSlotPrefix();
        int module = def.getModuleIndex();

        // 根据模块类型生成不同数量的 Slot
        switch (def.getKey()) {
            case "stories":
                slots.add(String.format("%s-%02d-%02d", prefix, module, posIndex));  // 封面图
                slots.add(String.format("AUD-%02d-%02d", module, posIndex));          // 朗读音频
                break;
            case "videos":
                slots.add(String.format("%s-%02d-%02d", prefix, module, posIndex));  // 封面图
                slots.add(String.format("VID-%02d-%02d", module, posIndex));          // 视频文件
                break;
            case "anatomies":
                slots.add(String.format("%s-%02d-%02d", prefix, module, posIndex));  // 图片
                slots.add(String.format("MDL-%02d-%02d", module, posIndex));          // 分体模型
                break;
            case "photoCompares":
                slots.add(String.format("%s-%02d-%02d", prefix, module, posIndex * 2 - 1));  // 老照片
                slots.add(String.format("%s-%02d-%02d", prefix, module, posIndex * 2));       // 新照片
                break;
            case "locations":
                slots.add(String.format("%s-%02d-%02d", prefix, module, posIndex));   // 封面图 IMG-02-xx
                slots.add(String.format("MDL-%02d-%02d", module, posIndex));           // 3D模型 MDL-02-xx
                slots.add(String.format("VID-%02d-%02d", module, posIndex));           // 全景视频 VID-02-xx
                break;
            default:
                slots.add(String.format("%s-%02d-%02d", prefix, module, posIndex));
                break;
        }
        return slots;
    }

    /**
     * 构建 YAML 块文本（用于追加到 application.yml）
     */
    private String buildYamlBlock(String moduleKey, int id, Map<String, String> formData) {
        StringBuilder sb = new StringBuilder();
        sb.append("    - id: ").append(id).append("\n");
        for (Map.Entry<String, String> e : formData.entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) {
                String kebab = camelToKebab(e.getKey());
                sb.append("      ").append(kebab).append(": \"").append(e.getValue().replace("\"", "\\\"")).append("\"\n");
            }
        }
        return sb.toString();
    }

    private static String camelToKebab(String camel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('-');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}
