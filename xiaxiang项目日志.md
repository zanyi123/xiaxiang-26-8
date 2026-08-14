# 薪火侨乡项目开发日志

> 记录时间：2026-08-10（初版）→ 2026-08-14（更新）
> 项目路径：d:\JAVA\xiaxiang
> 技术栈：Java 21 + Spring Boot 2.7.18 + Thymeleaf + COS对象存储

---

## 一、功能开发

### 1. MBTI测试交互优化
**问题**：MBTI测试所有步骤同时显示，未按选择顺序跳转

**解决方案**：
- 添加CSS样式控制`.life-panel`的显示/隐藏
- 通过`.active`类切换当前步骤的可见性
- 用户选择后延迟400ms自动跳转下一步

**修改文件**：
- `src/main/resources/templates/index.html`

**关键代码**：
```css
.life-panel { display: none; }
.life-panel.active { display: block; }
```

---

### 2. 历史时间线迁移
**问题**：首页时间线占用大块空间，影响首页布局

**解决方案**：
- 将历史时间线从首页移除
- 迁移至景区地图页面 `/map`，采用蜿蜒曲线形式
- 使用SVG `getPointAtLength()` 方法动态计算节点位置，自动对齐曲线
- 6个可点击节点：1644、1649、1840、1920s、2007、2026
- 节点按类型区分颜色：历史（蓝）、建筑（金）、荣誉（红）、实践（绿）

**修改文件**：
- [index.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/index.html)（移除时间线）
- [map.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/map.html)（添加蜿蜒时间线）
- [IndexController.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/controller/IndexController.java)（添加时间线数据传递）

---

### 3. 网站导览卡片优化（3个新入口）
**功能**：为网站导览的12个卡片优化内容，替换/新增3个入口

**变更**：
- `虚拟盖章` → `采访专栏`（图文单元，IMG-16-xx）
- `老照片对比` → `视频展播`（独立内容，VID-09-xx / IMG-09-xx）
- `趣味收集`（图片单元，IMG-17-xx，仅保留3分类：自然、人物纪实、团队纪实）

**新增页面**：
- [interview.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/interview.html)（采访专栏页 `/interview`）
- [collection.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/collection.html)（趣味收集页 `/collection`）
- [video-show.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/video-show.html)（视频展播页 `/video-show`）

**修改文件**：
- [application.yml](file:///d:/JAVA/xiaxiang/src/main/resources/application.yml)
- [AppProperties.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/properties/AppProperties.java)
- [SlotService.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/service/SlotService.java)
- [IndexController.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/controller/IndexController.java)
- [sitemap.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/sitemap.html)

**编号规则**：
- 视频展播：VID-09-xx（视频）、IMG-09-xx（封面）
- 采访专栏：IMG-16-xx（图文单元）
- 趣味收集：IMG-17-xx（图片单元）
- 建筑故事摄影集：IMG-18-xx（图片单元）

---

### 4. 建筑故事独立化
**问题**：建筑故事内容与景区导览图、时间线混合在同一页面

**解决方案**：
- 建筑故事改为独立页面 `/architecture-photo`
- 采用摄影集形式，4个分类：坊内现存碉楼、强亚村-老宅村碉楼群、碉楼院特写、古屋内部
- 古屋内部分类与景区地图"古屋纪实"（location ID=6）共享素材
- 从 map.html 中彻底移除建筑卡片CSS、JS变量和HTML

**新增文件**：
- [architecture-photo.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/architecture-photo.html)（建筑故事摄影集页）

**修改文件**：
- [IndexController.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/controller/IndexController.java)（新增 `/architecture-photo` 路由及素材共享逻辑）
- [map.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/map.html)（移除建筑卡片CSS/JS/HTML，清理 `buildings` 相关数据传递）

---

### 5. 视频展播独立页面
**功能**：将视频展播从首页移至独立页面

**实现**：
- 导航栏新增「视频展播」入口（位于「项目成果」和「关于我们」之间）
- 独立页面 `/video-show` 包含11个视频素材卡片
- 点击卡片弹出视频播放窗口，支持ESC键关闭

**修改文件**：
- [video-show.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/video-show.html)
- [index.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/index.html)（导航栏更新）

---

### 6. MBTI测试优化
**问题**：MBTI测试问题与建筑关联度低，结局建筑名称与地图节点不匹配

**解决方案**：
- 重新设计9个结局的建筑名称和跳转locationId，与景区地图6个节点对齐
- 优化测试问题，使选项与宝源坊建筑紧密关联（碉楼世家、青石板路等）
- 修复 `startTour` 函数名冲突导致的白屏问题（重命名为 `goToLocation`）

**修改文件**：
- [index.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/index.html)

---

### 7. 地图调试模式优化
**问题**：Ctrl+Shift+D快捷键与Chrome书签冲突

**解决方案**：
- 快捷键改为 `` ` ``（反引号）
- 新增右下角📍圆形按钮切换调试模式
- 调试模式开启时按钮有脉冲动画

**修改文件**：
- [map.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/map.html)

---

### 8. 后端内容管理优化
**功能**：后端模块列表与前端功能对齐

**模块调整**：
- 保留：知识库、民俗文化、实践日志、视频展播、建筑解剖、团队成员
- 重命名：`建筑故事` → `景区建筑`
- 新增：采访专栏、趣味收集、建筑故事摄影集
- 移除：老照片对比

**分类匹配修复**：
- FieldDef 增加 `optionLabels` 字段，实现 select 选项的「值-显示名」分离
- 建筑故事摄影集分类使用英文key（diaolou/village/courtyard/interior）存储，中文显示
- FieldAccessor 支持简单字段路径（如 `mapBackgroundImage`）

**修改文件**：
- [ContentManageService.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/service/ContentManageService.java)
- [FieldAccessor.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/service/FieldAccessor.java)
- [content.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/admin/content.html)
- [SlotService.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/service/SlotService.java)

---

### 9. 导航链接统一与旧名称清理
**问题**：全站导航链接使用 `#building-story` 锚点跳转，部分页面仍有旧名称残留

**解决方案**：
- 将所有页面的「建筑故事」导航链接从 `/#building-story` 统一改为 `/architecture-photo`
- 22个页面共39处链接更新
- 修复 interview.html 错误链接 `/stories` → `/architecture-photo`
- 清理 index.html、sitemap.html、qiaoyun.css 中「6栋特色建筑」等旧名称
- 清理 map.html 中不再使用的 buildings CSS、JS变量、viewBuilding函数
- 清理 IndexController.map() 中不再传递的 buildings 数据

**修改文件**（23个HTML模板 + 1个Java + 1个CSS）：
- [index.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/index.html)
- [map.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/map.html)
- [sitemap.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/sitemap.html)
- [about.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/about.html)
- [anatomy.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/anatomy.html)
- [archive.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/archive.html)
- [archive-detail.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/archive-detail.html)
- [blog-detail.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/blog-detail.html)
- [dialect.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/dialect.html)
- [error.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/error.html)
- [knowledge.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/knowledge.html)
- [knowledge-detail.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/knowledge-detail.html)
- [location.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/location.html)
- [photo-compare.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/photo-compare.html)
- [qiaopi.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/qiaopi.html)
- [qiaopi-detail.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/qiaopi-detail.html)
- [quiz.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/quiz.html)
- [search-result.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/search-result.html)
- [stamps.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/stamps.html)
- [stories.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/stories.html)
- [story-detail.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/story-detail.html)
- [video-detail.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/video-detail.html)
- [video-show.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/video-show.html)
- [interview.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/interview.html)
- [upload.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/admin/upload.html)
- [IndexController.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/controller/IndexController.java)
- [qiaoyun.css](file:///d:/JAVA/xiaxiang/src/main/resources/static/css/qiaoyun.css)

---

## 二、Bug修复

### 问题1：MBTI测试入口跳转错误
**表现**：点击「商贸之家」卡片跳转到答题页面

**修复**：
- 修改卡片href为`javascript:void(0)`
- 绑定`selectOriginFromPortal()`函数
- 切换导览模式为内容模式，触发出身选择逻辑

**修改文件**：
- [index.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/index.html)

---

### 问题2：云游宝源坊白屏/系统繁忙
**表现**：首页云游宝源坊区块点击后白屏

**根因**：前端存在两个 `startTour` 函数定义，参数传递冲突导致后端抛出 `NumberFormatException: For input string: "undefined"`

**修复**：
- 将MBTI测试结果中的 `startTour(locationId)` 重命名为 `goToLocation(locationId)`
- 更新所有调用处

**修改文件**：
- [index.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/index.html)

---

### 问题3：IMG-02-01 地图底图素材无法在后台显示
**表现**：素材匹配中心找不到地图底图编号

**根因**：FieldAccessor.java 仅支持数组索引路径（如 `foo[123].bar`），不支持简单字段路径（如 `mapBackgroundImage`）

**修复**：
- FieldAccessor 增加简单字段路径支持
- SlotService 调整 Module 02 顺序，IMG-02-01 排在地点图片之前

**修改文件**：
- [FieldAccessor.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/service/FieldAccessor.java)
- [SlotService.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/service/SlotService.java)

---

### 问题4：首页Thymeleaf模板语法错误
**表现**：首页内容缺失

**根因**：`th:style` 和 `th:classappend` 表达式中使用了字符串拼接

**修复**：
- 替换为Thymeleaf字面量替换语法 `|...|` 和 `${...}`

**修改文件**：
- [index.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/index.html)

---

## 三、编译与兼容性问题

### Maven打包错误
**错误信息**：
```
[ERROR] Error reading archive file: error in opening zip file
```

**原因**：旧的jar文件被本地Java进程占用

**解决方案**：
1. 停掉本地运行的Spring Boot程序：`taskkill /F /PID <pid>`
2. 清理target目录：`rmdir /s /q target`
3. 重新打包：`mvn clean package -DskipTests`

---

## 四、项目约束与规范

### 硬约束
- 后端管理部分不得修改
- 素材填充状态必须真实反映COS存储桶内容
- Java应用必须使用8080端口，冲突时先清理
- Nginx必须转发xinhuoqiaoyun.online到8080端口
- 历史线节点位置使用SVG getPointAtLength() 动态计算
- 视频展播入口必须在导航栏（「项目成果」和「关于我们」之间）
- 地图底图专用编号IMG-02-01，地点图片从IMG-02-02开始
- 地图调试模式：点击📍按钮或按反引号键

### 编号规则（2026-08-14更新）
| 模块 | 编号前缀 | 说明 |
|------|----------|------|
| 景区地点 | IMG-02-02~ | 01为地图底图 |
| 视频展播 | VID-09-xx / IMG-09-xx | 视频+封面 |
| 采访专栏 | IMG-16-xx | 图文单元 |
| 趣味收集 | IMG-17-xx | 图片单元 |
| 建筑故事摄影集 | IMG-18-xx | 图片单元 |

### 工程规范
- 3D模型加载三级降级：3D模型 → 部分图片 → DOM占位符
- SlotService 验证 YAML字段 + COS文件存在性
- CosService 实现文件存在性检查缓存
- 静态资源URL添加版本号（如 `?v=20260814`）
- Spring Boot 模板缓存禁用（Thymeleaf cache: false）
- Session 管理：单账号单会话，新登录踢掉旧会话
- 导览卡片采用图片全背景模式（背景图层+渐变遮罩+文字层+金色徽章）

---

## 五、部署与运维

### 本地一键启动脚本
**新增脚本**（项目根目录）：
- `启动.bat`：清理端口占用 → 清理target → Maven编译 → 启动服务
- `重启.bat`：停止 → 清理 → 编译 → 启动
- `停止.bat`：停止Java进程

### 服务器部署流程
1. `mvn clean package -DskipTests` 打包
2. WinSCP上传 `target/xiaxiang-building-tour-1.0-SNAPSHOT.jar` 到 `/opt/xiaxiang/`
3. OrcaTerm执行：
   ```bash
   pkill -f xiaxiang-building-tour
   cd /opt/xiaxiang && nohup java -jar xiaxiang-building-tour-1.0-SNAPSHOT.jar --spring.profiles.active=prod > app.log 2>&1 &
   echo $! > app.pid
   ```

---

## 六、文件变更清单（2026-08-14）

### 修改文件（32个）
| # | 文件路径 | 变更类型 |
|---|---------|---------|
| 1 | `src/main/java/.../controller/IndexController.java` | 重写（+79行） |
| 2 | `src/main/java/.../properties/AppProperties.java` | 修改（+50行） |
| 3 | `src/main/java/.../service/ContentManageService.java` | 重构（+166行） |
| 4 | `src/main/java/.../service/FieldAccessor.java` | 新增功能（+11行） |
| 5 | `src/main/java/.../service/SlotService.java` | 重构（+147行） |
| 6 | `src/main/resources/application.yml` | 大量修改（+331行） |
| 7 | `src/main/resources/static/css/qiaoyun.css` | 样式调整（+36行） |
| 8 | `src/main/resources/templates/index.html` | 大幅更新（+945行） |
| 9 | `src/main/resources/templates/map.html` | 清理+优化（+595行） |
| 10 | `src/main/resources/templates/sitemap.html` | 卡片优化（+126行） |
| 11-32 | 其余22个HTML模板 | 导航链接统一更新 |

### 新增文件
| 文件 | 说明 |
|------|------|
| `architecture-photo.html` | 建筑故事摄影集页 |
| `interview.html` | 采访专栏页 |
| `collection.html` | 趣味收集页 |
| `video-show.html` | 视频展播页 |
| `sitemap-interview.jpg` | 采访专栏卡片底图 |
| `sitemap-video-show.jpg` | 视频展播卡片底图 |
| `sitemap-collection.jpg` | 趣味收集卡片底图 |
| `启动.bat` / `重启.bat` / `停止.bat` | 一键启动脚本 |

---

## 七、待办事项

1. ✅ 建筑故事独立化完成
2. ✅ 网站导览3个新入口完成
3. ✅ 导航链接统一完成
4. ✅ 旧名称清理完成
5. 🔲 服务器部署与线上验证
6. 🔲 收集素材并上传COS存储桶
7. 🔲 地图节点坐标最终调整
8. 🔲 MBTI测试结果最终验证
