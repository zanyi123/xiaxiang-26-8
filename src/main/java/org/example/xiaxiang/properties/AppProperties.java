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
}