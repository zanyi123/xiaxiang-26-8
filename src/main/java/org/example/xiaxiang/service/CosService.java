package org.example.xiaxiang.service;

import com.qcloud.cos.COSClient;
import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.exception.BusinessException;
import org.example.xiaxiang.properties.AppProperties;
import org.example.xiaxiang.properties.CosProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * COS URL 生成服务
 *
 * 由于 COS 桶防盗链限制，所有 URL 返回后端代理路径 /cos/{key}
 * 由 CosProxyController 代理转发文件
 *
 * Mock 模式：app.mock-mode=true 时返回本地静态资源路径，用于开发调试
 */
@Slf4j
@Service
public class CosService {

    @Autowired
    private CosProperties cosProperties;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private COSClient cosClient;

    private String cosBaseUrl;

    /** 文件存在性缓存：避免每次页面请求都触发COS远程检查（SDK会对404打ERROR日志） */
    private final Map<String, Boolean> existenceCache = new ConcurrentHashMap<>();

    /**
     * 初始化 COS 基础 URL
     */
    @PostConstruct
    public void init() {
        String region = cosProperties.getRegion();
        String bucket = cosProperties.getBucketName();

        if (!isBlank(region) && !isBlank(bucket)) {
            // 拼接标准 COS 公网访问域名
            this.cosBaseUrl = String.format("https://%s.cos.%s.myqcloud.com/", bucket, region);
            log.info("[CosService] COS 基础 URL 初始化完成：{}", this.cosBaseUrl);
        } else {
            log.warn("[CosService] COS 配置不完整（region 或 bucket 为空），真实 COS URL 将不可用");
            this.cosBaseUrl = "";
        }
    }

    /**
     * 获取文件的访问 URL
     * 由于 COS 桶防盗链限制，返回后端代理 URL
     *
     * @param key COS 对象键，如 "models/kaiping.splat"
     * @return 代理 URL
     */
    public String getFileUrl(String key) {
        if (isBlank(key)) {
            throw new BusinessException("文件 key 不能为空");
        }

        // Mock 模式：返回本地静态资源路径
        if (appProperties.isMockMode()) {
            String mockUrl = "/mock/" + key;
            log.debug("[CosService] Mock 模式返回本地 URL：{}", mockUrl);
            return mockUrl;
        }

        // 返回后端代理 URL（绕过 COS 防盗链）
        return "/cos/" + key;
    }

    /**
     * 安全获取文件 URL：key 为空或异常时返回 null，不抛异常
     * 适用于素材可选场景（如封面图未上传时回退到 AI 生成图）
     *
     * @param key COS 对象键，可为 null
     * @return 代理 URL，或 null
     */
    public String getUrlSafely(String key) {
        if (isBlank(key)) {
            return null;
        }
        // Mock 模式：返回本地静态资源路径
        if (appProperties.isMockMode()) {
            return "/mock/" + key;
        }
        // 返回后端代理 URL（绕过 COS 防盗链）
        return "/cos/" + key;
    }

    // ==================== 建筑相关方法 ====================

    /**
     * 根据建筑 ID 获取 3D 模型 URL
     */
    public String getModelUrl(Integer buildingId) {
        AppProperties.Building building = findBuildingById(buildingId);
        if (building == null) {
            throw new BusinessException("建筑不存在：ID=" + buildingId);
        }
        return getUrlSafely(building.getModelKey());
    }

    /**
     * 根据建筑 ID 获取 4K 视频 URL
     */
    public String getVideoUrl(Integer buildingId) {
        AppProperties.Building building = findBuildingById(buildingId);
        if (building == null) {
            throw new BusinessException("建筑不存在：ID=" + buildingId);
        }
        return getUrlSafely(building.getVideoKey());
    }

    /**
     * 根据建筑 ID 获取封面图 URL
     */
    public String getCoverImageUrl(Integer buildingId) {
        AppProperties.Building building = findBuildingById(buildingId);
        if (building == null) {
            throw new BusinessException("建筑不存在：ID=" + buildingId);
        }
        return getUrlSafely(building.getCoverImage());
    }

    /**
     * 查找建筑信息
     */
    public AppProperties.Building findBuildingById(Integer buildingId) {
        if (buildingId == null || appProperties.getBuildings() == null) {
            return null;
        }
        return appProperties.getBuildings().stream()
                .filter(b -> buildingId.equals(b.getId()))
                .findFirst()
                .orElse(null);
    }

    // ==================== 地点相关方法（景区导航） ====================

    /**
     * 获取所有地点列表
     */
    public java.util.List<AppProperties.Location> getAllLocations() {
        if (appProperties.getLocations() == null) {
            return java.util.Collections.emptyList();
        }
        return appProperties.getLocations();
    }

    /**
     * 根据地点 ID 获取地点信息
     */
    public AppProperties.Location findLocationById(Integer locationId) {
        if (locationId == null || appProperties.getLocations() == null) {
            return null;
        }
        return appProperties.getLocations().stream()
                .filter(l -> locationId.equals(l.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据地点 ID 获取 3D 模型 URL
     */
    public String getLocationModelUrl(Integer locationId) {
        AppProperties.Location location = findLocationById(locationId);
        if (location == null) {
            throw new BusinessException("地点不存在：ID=" + locationId);
        }
        return getUrlSafely(location.getModelKey());
    }

    /**
     * 根据地点 ID 获取图片 URL
     */
    public String getLocationImageUrl(Integer locationId) {
        AppProperties.Location location = findLocationById(locationId);
        if (location == null) {
            return null;
        }
        return getUrlSafely(location.getImageKey());
    }

    /**
     * 根据地点 ID 获取视频 URL
     */
    public String getLocationVideoUrl(Integer locationId) {
        AppProperties.Location location = findLocationById(locationId);
        if (location == null) {
            return null;
        }
        return getUrlSafely(location.getVideoKey());
    }

    /**
     * 根据地点 ID 获取 AI 讲解文本
     */
    public String getLocationAudioText(Integer locationId) {
        AppProperties.Location location = findLocationById(locationId);
        if (location == null) {
            throw new BusinessException("地点不存在：ID=" + locationId);
        }
        return location.getAudioText();
    }

    // ==================== 工具方法 ====================

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 检查指定 key 的素材是否真实存在（本地mock或COS远程）
     * 带缓存：同一个key只查一次COS，后续直接用缓存结果，避免SDK重复打ERROR日志
     *
     * @param key 素材 key，如 "images/diaolou.jpg"
     * @return true = 真实存在；false = 不存在或无法验证
     */
    public boolean fileExists(String key) {
        if (isBlank(key)) return false;

        // 缓存命中直接返回
        Boolean cached = existenceCache.get(key);
        if (cached != null) {
            return cached;
        }

        // Mock 模式：检查本地文件
        if (appProperties.isMockMode()) {
            String localPath = "src/main/resources/static/mock/" + key;
            File f = new File(localPath);
            boolean exists = f.exists() && f.isFile();
            existenceCache.put(key, exists);
            return exists;
        }

        // 真实 COS 模式：通过 HEAD 请求检查
        try {
            boolean exists = cosClient.doesObjectExist(cosProperties.getBucketName(), key);
            existenceCache.put(key, exists);
            return exists;
        } catch (Exception e) {
            // COS返回404 → 文件不存在（这是预期行为，缓存为false避免重复请求）
            existenceCache.put(key, false);
            log.debug("[CosService] COS文件不存在或检查异常 {} => {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 清除文件存在性缓存（上传/删除素材后调用）
     */
    public void clearCache() {
        existenceCache.clear();
        log.info("[CosService] 文件存在性缓存已清除");
    }

    /**
     * 通用素材 URL Map 构造器
     * 遍历列表，将每个对象的素材 key 转换为 COS URL，按 id 索引返回
     * 关键修复：只有素材在COS中真实存在时才放入Map，不存在的key跳过
     *
     * @param list      数据列表
     * @param idGetter  id 提取函数
     * @param keyGetter 素材 key 提取函数
     * @param <T>       数据类型
     * @return Map<id, COS URL>  只包含真实存在的素材
     */
    public <T> java.util.Map<Integer, String> buildUrlMap(java.util.List<T> list,
                                                          java.util.function.Function<T, Integer> idGetter,
                                                          java.util.function.Function<T, String> keyGetter) {
        java.util.Map<Integer, String> map = new java.util.HashMap<>();
        if (list == null) {
            return map;
        }
        for (T item : list) {
            Integer id = idGetter.apply(item);
            String key = keyGetter.apply(item);
            String url = getUrlSafely(key);
            if (id != null && url != null) {
                // 关键修复：验证文件真实存在
                if (fileExists(key)) {
                    map.put(id, url);
                } else {
                    log.debug("[CosService] 跳过不存在的素材 key={} (id={})", key, id);
                }
            }
        }
        return map;
    }
}