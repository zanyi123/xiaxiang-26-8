package org.example.xiaxiang.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用级配置映射类
 * 与 application.yml 中的 app.* 前缀配置绑定
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Mock 模式开关：true 时返回本地静态资源 URL */
    private boolean mockMode = true;

    /**
     * 运行环境：dev / prod
     * 用于区分本地开发和正式部署：
     * - dev: 本地（本地IDEA启动，禁止对COS桶和yml做写入操作，防止污染正式环境
     * - prod: 正式服务器，允许所有上传/绑定/解绑操作
     * 服务器 application.yml 必须显式配置 app.env: prod
     */
    private String env = "dev";

    public boolean isProdEnv() {
        return "prod".equalsIgnoreCase(env);
    }

    /** 建筑静态数据列表 */
    private List<Building> buildings;

    /** 核心地点列表（用于景区导航） */
    private List<Location> locations;

    /** 侨乡故事列表 */
    private List<Story> stories;

    /** 历史时间线 */
    private List<TimelineItem> timeline;

    /** 田野档案数据 */
    private List<ArchiveItem> archives;

    /** 建筑知识库 */
    private List<KnowledgeItem> knowledge;

    /** 民俗文化 */
    private List<CultureItem> cultures;

    /** 实践日志 */
    private List<BlogPost> blogPosts;

    /** 视频展播 */
    private List<VideoItem> videos;

    /** 团队成员 */
    private List<TeamMember> team;

    /** 建筑解剖数据 */
    private List<BuildingAnatomy> anatomies;

    /** 老照片对比数据 */
    private List<PhotoCompare> photoCompares;

    /** 侨批文化数据 */
    private List<QiaopiItem> qiaopi;

    /** 方言学习数据 */
    private List<DialectItem> dialects;

    /** 知识答题数据 */
    private List<QuizQuestion> quizzes;

    /** 虚拟盖章数据 */
    private List<StampItem> stamps;

    /** 项目成果数据 */
    private List<AchievementItem> achievements;

    /** 采访专栏数据（Module 16） */
    private List<InterviewItem> interviews;

    /** 趣味收集数据（Module 17） */
    private List<CollectionItem> collections;

    /** 建筑故事摄影集数据（Module 18） */
    private List<ArchitecturePhotoItem> architecturePhotos;

    /** 景区导航地图底图（IMG-02-01） */
    private String mapBackgroundImage;

    /**
     * 建筑信息子对象
     */
    @Data
    public static class Building {
        private Integer id;
        private String name;
        private String description;
        private String coverImage;
        private String modelKey;
        private String videoKey;
    }

    /**
     * 地点信息子对象（景区导航用）
     */
    @Data
    public static class Location {
        private Integer id;
        private String number;
        private String name;
        private String description;
        private String history;
        private Double xCoordinate;
        private Double yCoordinate;
        private String modelKey;
        private String imageKey;
        private String videoKey;
        private String audioText;
    }

    /**
     * 侨乡故事
     */
    @Data
    public static class Story {
        private Integer id;
        private String title;
        private String category;
        private String summary;
        private String content;
        private String author;
        private String date;
        private String coverImage;
        private String audioKey;
        private Integer views;
    }

    /**
     * 历史时间线
     */
    @Data
    public static class TimelineItem {
        private Integer id;
        private String year;
        private String title;
        private String description;
        private String type;
    }

    /**
     * 田野档案
     */
    @Data
    public static class ArchiveItem {
        private Integer id;
        private String title;
        private String category;
        private String description;
        private String icon;
        private String count;
        private String unit;
        private String detailUrl;
    }

    /**
     * 建筑知识库
     */
    @Data
    public static class KnowledgeItem {
        private Integer id;
        private String title;
        private String category;
        private String summary;
        private String content;
        private String coverImage;
        private String difficulty;
    }

    /**
     * 民俗文化
     */
    @Data
    public static class CultureItem {
        private Integer id;
        private String name;
        private String category;
        private String description;
        private String coverImage;
    }

    /**
     * 实践日志
     */
    @Data
    public static class BlogPost {
        private Integer id;
        private String title;
        private String summary;
        private String author;
        private String date;
        private String coverImage;
        private String category;
    }

    /**
     * 视频展播
     */
    @Data
    public static class VideoItem {
        private Integer id;
        private String title;
        private String description;
        private String videoKey;
        private String coverImage;
        private String duration;
        private Integer views;
    }

    /**
     * 团队成员
     */
    @Data
    public static class TeamMember {
        private Integer id;
        private String name;
        private String role;
        private String major;
        private String avatar;
        private String bio;
    }

    /**
     * 建筑解剖（部位分解展示）
     */
    @Data
    public static class BuildingAnatomy {
        private Integer id;
        private Integer buildingId;
        private String partName;
        private String partNameEn;
        private String category;
        private String description;
        private String function;
        private String material;
        private String era;
        private String imageKey;
        private String modelKey;
    }

    /**
     * 老照片对比
     */
    @Data
    public static class PhotoCompare {
        private Integer id;
        private String title;
        private String location;
        private String yearOld;
        private String yearNew;
        private String description;
        private String oldImageKey;
        private String newImageKey;
        private String story;
    }

    /**
     * 侨批文化
     */
    @Data
    public static class QiaopiItem {
        private Integer id;
        private String title;
        private String sender;
        private String recipient;
        private String sendFrom;
        private String year;
        private String amount;
        private String content;
        private String translation;
        private String imageKey;
        private String category;
    }

    /**
     * 方言学习
     */
    @Data
    public static class DialectItem {
        private Integer id;
        private String chinese;
        private String dialect;
        private String pinyin;
        private String meaning;
        private String example;
        private String audioKey;
        private String category;
    }

    /**
     * 知识答题
     */
    @Data
    public static class QuizQuestion {
        private Integer id;
        private String question;
        private List<String> options;
        private Integer answer;
        private String explanation;
        private String category;
        private String difficulty;
    }

    /**
     * 虚拟盖章
     */
    @Data
    public static class StampItem {
        private Integer id;
        private String name;
        private String location;
        private String description;
        private String imageKey;
        private String unlockCondition;
        private String rarity;
    }

    /**
     * 项目成果
     */
    @Data
    public static class AchievementItem {
        private Integer id;
        private String icon;
        private String title;
        private String description;
    }

    /**
     * 采访专栏（Module 16）
     */
    @Data
    public static class InterviewItem {
        private Integer id;
        private String title;
        private String subtitle;
        private String summary;
        private String content;
        private String category;
        private String date;
        private String coverImage;
    }

    /**
     * 趣味收集（Module 17）
     */
    @Data
    public static class CollectionItem {
        private Integer id;
        private String title;
        private String description;
        private String category;
        private String imageKey;
    }

    /**
     * 建筑故事摄影集（Module 18）
     */
    @Data
    public static class ArchitecturePhotoItem {
        private Integer id;
        private String title;
        private String description;
        private String category;
        private String imageKey;
    }
}