# 薪火侨乡项目开发日志

> 记录时间：2026-08-01（初次） / 2026-08-03（更新）  
> 项目路径：`d:\JAVA\xiaxiang`  
> 技术栈：Spring Boot + Thymeleaf + 腾讯云COS + 3D高斯泼溅(3DGS) + Three.js

---

## 一、项目架构与基础建设

### 1.1 主题层级与导航重构
**问题**：原导航栏包含"两大方案""宝源坊专题"等旧入口，品牌定位模糊，"薪火侨乡"主品牌与"宝源坊"子专题关系不清。

**解决**：
- 明确"薪火侨乡"为主品牌，"宝源坊"为子专题
- 导航栏统一重构为：首页 | 网站导览 | 云游侨乡 | 我的侨乡人生 | 建筑故事 | 项目成果 | 关于我们
- 修改23个前端模板的导航栏和页脚，统一管理入口

**涉及文件**：全部 `templates/*.html`

---

### 1.2 无数据库身份认证系统
**问题**：需要后台管理权限系统，但不想引入数据库破坏现有架构。

**解决**：
- 基于YAML配置账号信息（3名管理员 + 12名实践同学）
- 使用HttpSession管理会话，拦截器实现后台接口鉴权
- 账号规则：管理员`名字拼音+00+排序数字`（前3），其他按姓氏排序到15
- 密码规则：`Sanxiaxiang` + 排序数字

**涉及文件**：`AuthController.java`、`application.yml`

---

### 1.3 素材精准匹配（Slot编号体系）
**问题**：素材上传后粗分类存到COS桶，但具体内容细传到网站指定位置可能传错。

**解决**：
- 为网站每个素材空位定义唯一编号（如`IMG-02-01`），规则：类型+二级入口+三级标题+数字编号
- 枚举98个Slot，覆盖15个模块
- 开发"素材匹配中心"Tab页：左列Slot编号、中列填充状态、右列素材选择窗口
- YAML热更新：`YamlUpdater`工具类直接修改配置文件，无需重启

**涉及文件**：`SlotService.java`、`upload.html`

---

### 1.4 部署流程优化
**问题**：需要规范的本地打包→服务器部署流程。

**解决**：
- 创建`deploy-local.ps1`（本地打包上传）和`deploy-server.sh`（服务器部署）
- 关键：`application.yml`必须与JAR包同级存放，YAML热更新才能生效
- 流程：`mvn clean package` → 上传jar+yml → 重启服务

---

## 二、三级功能开发

### 2.1 GitHub备份
- 提交ID：`4b2c32a`
- 排除敏感文件（application.yml）和大体积3D重建数据

### 2.2 建筑解剖3D分解
**问题**：原左侧为AI生图，需改为3D模型展示。

**解决**：
- `AnatomyController.java`：构建部位3D模型URL Map和图片URL Map，替换原cosService直传
- `anatomy.html`：左侧AI生图→3D canvas，添加部位卡片数据属性
- `anatomy-3d.js`：3D场景初始化、部位切换、三级fallback策略
  1. 3D模型（.splat文件）
  2. 部位图片
  3. 示例立方体+提示文字

**涉及文件**：[AnatomyController.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/controller/AnatomyController.java)、[anatomy.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/anatomy.html)、[anatomy-3d.js](file:///d:/JAVA/xiaxiang/src/main/resources/static/js/anatomy-3d.js)

### 2.3 AI讲解API与TTS优化
**问题**：讲解声线太僵硬，需要更自然的语音。

**解决**：
- `AiController.java`：统一讲解文本接口`/api/ai/guide?type=location&id=X`，支持多种类型
- `qiaoyun-tts.js`：智能选择最佳中文女声，支持文本分段播放、暂停、继续、停止
- 优先播放真实音频，TTS作为后备

**涉及文件**：[AiController.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/controller/AiController.java)、[qiaoyun-tts.js](file:///d:/JAVA/xiaxiang/src/main/resources/static/js/qiaoyun-tts.js)

---

## 三、Bug修复与优化

### 3.1 素材填充状态显示错误（重大问题）
**问题**：后台素材填补界面显示"全部填充"，但COS桶里实际上没有素材。

**根因分析**：
1. `SlotService`只看YAML字段是否有值，未验证COS文件真实存在
2. `mock-mode: true`导致CosService返回本地mock路径，掩盖COS空事实
3. 网站模板用trae-api兜底图，显示假图片而非Slot编号

**修复**：
| # | 修复项 | 文件 |
|---|--------|------|
| 1 | `CosService.fileExists()`用`doesObjectExist`远程检查 | CosService.java |
| 2 | SlotService：YAML有值但COS不存在→标记空缺+"(未上传)" | SlotService.java |
| 3 | `buildUrlMap`只有COS真实存在的素材才放入URL Map | CosService.java |
| 4 | 关闭`mock-mode: false` | application.yml |
| 5 | 前端7个页面trae-api兜底图→Slot编号占位符 | index/archive/about/stories/qiaopi/anatomy/location.html |
| 6 | 统一slot-placeholder CSS样式 | qiaoyun.css |
| 7 | 占位符JS自动扫描工具 | slot-placeholder.js |

**结果**：98个Slot全部正确标记（Filled: 0, Empty: 98）

### 3.2 COS 404日志刷屏
**问题**：每次访问页面，COS SDK对不存在的文件发HEAD请求，报大量404 ERROR日志。

**修复**：
- `application.yml`中`com.qcloud.cos`日志级别设为`WARN`
- `CosService`添加文件存在性缓存，首次检查后缓存结果，后续直接读取
- 效果：第一次访问1条ERROR（SDK内部行为），后续零日志

**涉及文件**：[CosService.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/service/CosService.java)、[application.yml](file:///d:/JAVA/xiaxiang/src/main/resources/application.yml)

### 3.3 端口8080被占用
**问题**：IDEA启动时报`Port 8080 was already in use`。

**解决**：
```
netstat -ano | findstr :8080
taskkill /PID 对应PID /F
```
根因：之前命令行启动的Java进程（PID=29792）仍在后台运行。

### 3.4 后台入口缺失
**问题**：导航栏没有后台登录入口。

**修复**：
- 22个页面导航栏末尾添加`<a href="/admin">后台登录</a>`
- `AuthController`新增`/admin`根路由，自动重定向到`/admin/login`

**涉及文件**：全部`templates/*.html`、[AuthController.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/controller/AuthController.java)

### 3.5 MBTI"探索对应建筑"跳转错误
**问题**：`startTour()`只跳`/map`，没有根据MBTI结果跳转到对应具体建筑。

**修复**：
- 9个结局添加`locationId`字段，映射到实际地点：

| 结局 | 建筑名 | 跳转 |
|------|--------|------|
| 远赴南洋/归乡兴家/侨领风范 | 瑞石楼/宝源门楼 | `/location/1` |
| 重振家业/文化传承者 | 宝源邓公祠 | `/location/5` |
| 一代商人 | 瑞石楼碉楼 | `/location/6` |
| 乡村新主人 | 古厝 | `/location/2` |
| 匠心营造 | 教堂 | `/location/3` |
| 薪火相传 | 风雨亭 | `/location/4` |

- 新增`exploreBuilding(locationId)`函数

**涉及文件**：[index.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/index.html#L507-L509)

### 3.6 AI朗读暂停/停止效果无区别
**问题**：暂停和停止都会重置语音，暂停后无法从断点继续。

**根因**：location.html和map.html用原生`speechSynthesis`，`pauseGuide()`实际调用了`cancel()`销毁一切。

**修复**：两个页面改为使用QiaoyunTTS：

| 操作 | 暂停 | 停止 |
|------|------|------|
| 行为 | `pause()` 保留播放队列 | `stop()` 清空队列 |
| 按钮变化 | "暂停"→"继续播放" | 保持不变 |
| 再点播放 | 从暂停处继续 | 从头播放 |

**涉及文件**：[location.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/location.html#L148-L202)、[map.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/map.html#L107-L208)

---

## 四、编译问题与Java 8兼容性

### 4.1 FileWriter构造函数不支持Charset
- **问题**：Java 8中`FileWriter(File, Charset)`不存在
- **解决**：`OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)`

### 4.2 String新方法不存在
- **问题**：`stripLeading()`、`isBlank()`、`repeat()`是Java 11+方法
- **解决**：自定义工具方法，`trim().isEmpty()`替代`isBlank()`

### 4.3 DialectItem方法名不匹配
- **问题**：`getPhrase()`方法不存在
- **解决**：改为`getChinese()`

---

## 五、3D场景修复

### 5.1 canvas ID不匹配
- building-3d.js中ID为`3d-canvas`，building.html中为`canvas` → 统一为`3d-canvas`

### 5.2 CDN版本冲突
- Three.js r128与OrbitControls r0.150.0不兼容 → 统一使用r128

### 5.3 模型加载问题
- `window.modelUrl`未定义 → 挂载到window对象
- `THREE.GSplatLoader`在r128中不存在 → fetch探测文件存在性
- `xhr.loaded / xhr.total`当total为0产生NaN → 添加除零保护

---

## 六、项目约束与规范

### 硬约束
1. 后台管理部分已完成，不得修改
2. 素材填充状态必须严格反映COS桶真实内容，自动填充色块不算已填充
3. 未填充素材的位置必须在网站显示其Slot编号

### 工程规范
1. 3D模型加载三级fallback：3D模型 → 部位图片 → 示例立方体
2. SlotService必须同时验证YAML字段和COS文件存在性
3. 前端使用统一slot-placeholder样式
4. COS SDK日志级别设为WARN
5. CosService对文件存在性检查实现缓存

### 经验教训
- mock-mode会掩盖COS空事实，生产环境必须关闭
- COS SDK的404 ERROR是预期行为（检查文件是否存在），不应视为真正的错误
- 端口占用时先排查后台残留进程

---

## 七、后台管理访问

| 环境 | 地址 |
|------|------|
| 本地 | `http://localhost:8080/admin/login` |
| 服务器 | `http://服务器IP:8080/admin/login` |
| 域名 | `http://域名/admin/login` |

管理员账号：`huangkunshui001` / 密码：`Sanxiaxiang1`

---

## 八、待办事项

- [ ] MBTI"分享功能"已实现（Web Share API），可进一步优化分享卡片样式
- [ ] "开始云游"按钮与导航栏"云游侨乡"存在功能重叠，可考虑合并或改为图标入口
- [ ] 服务器正式部署后验证所有功能

---

## 九、素材渲染普遍生效（2026-08-03）

### 9.1 问题1：素材绑定后网站缺口未实时更新（全类型普遍生效）

**问题**：素材绑定后网站对应素材缺口未更新，且需覆盖图片、视频、3D模型、音频所有类型，不单只是某块HTML。

**根因**：
1. 多个Controller未传递URL Map到前端
2. 多个详情页/列表页使用AI生成图作为fallback，掩盖了Slot占位符
3. 3D模型加载失败时显示示例立方体，无Slot编号提示
4. 方言/视频等页面的素材缺口未显示Slot编号

**修复（全链路闭环）**：

#### 后端：所有Controller补齐URL Map传递
| Controller | 新增传递 | 对应Slot |
|---|---|---|
| StampController | `imageUrls`(IMG-15) | 印章图标 |
| AboutController | `teamAvatarUrls`(IMG-10) + `blogCoverUrls`(IMG-08) + `videoCoverUrls`(IMG-09) | 团队/日志/视频 |
| KnowledgeController | `coverUrls`(IMG-06) + `cultureCoverUrls`(IMG-07) | 知识/文化 |
| LocationController / StoryController / PhotoController / QiaopiController / AnatomyController / DialectController | 前几轮已补齐 | 各模块 |

**涉及文件**：[StampController.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/controller/StampController.java)、[AboutController.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/controller/AboutController.java) 等

#### 前端：全素材类型Slot占位符（绑定后实时替换）

| 类型前缀 | 模块 | 页面 | 关键实现 |
|---|---|---|---|
| IMG-01~IMG-15 | 15个模块 | 所有列表页+详情页 | `th:if` 绑定url有值时渲染`<img>`，否则渲染带`data-slot-id`的`.slot-placeholder`+Slot编号徽章 |
| VID-01, VID-04 | 建筑/地点视频 | building.html, location.html | 优先3D模型→fallback视频→两者都无显示Slot占位符 |
| MDL-01, MDL-02, MDL-05 | 建筑/地点/解剖3D模型 | building-3d.js, anatomy-3d.js | 所有模型加载失败/超时/无素材分支不再显示示例立方体，统一回退到DOM Slot占位符 |
| AUD-03, AUD-13 | 故事/方言录音 | story-detail.html, dialect.html | 未上传时禁用播放按钮，显示AUD-xx-xx占位徽章 |

**涉及文件**：
- [anatomy.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/anatomy.html)：新增`anatomy-slot-placeholder`容器，传递`partIndex`
- [anatomy-3d.js](file:///d:/JAVA/xiaxiang/src/main/resources/static/js/anatomy-3d.js)：新增`showAnatomySlotPlaceholder()`，所有失败分支改为DOM Slot占位符（MDL-05-xx/IMG-05-xx双编号）
- [dialect.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/dialect.html)：AUD-13录音占位提示
- [video-detail.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/video-detail.html)：相关视频AI fallback→IMG-09占位符
- [knowledge.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/knowledge.html)：知识/文化列表AI fallback→IMG-06/IMG-07占位符
- [about.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/about.html)：团队成员头像→IMG-10占位符
- [stamps.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/stamps.html)：印章图标→IMG-15占位符+首字兜底

#### AI fallback全面清理
移除所有`trae-api-cn.mchost.guru`的AI生成图fallback，改为Slot占位符。所有页面引入[slot-placeholder.js](file:///d:/JAVA/xiaxiang/src/main/resources/static/js/slot-placeholder.js)。

**结果**：编译通过（33 source files, 0 errors），任何素材Slot绑定后yml实时更新→Thymeleaf重渲染→占位符替换为真实素材，全站生效无需重启。

---

## 十、配置管理重构（2026-08-03）

### 10.1 问题：服务器后台登录报错"连接被拒绝"

**问题**：更新服务器代码后，正式网站后台登录界面报`ERR_CONNECTION_REFUSED`。

**根因**：`.gitignore`中`application.yml`被忽略，导致服务器`git pull`时配置文件丢失，Spring Boot启动时COS密钥为null→CosConfig初始化失败→端口8080无服务监听→连接被拒绝。

### 10.2 配置管理方案重构

**旧方案（有缺陷）**：
- `application.yml`被.gitignore忽略
- 服务器上配置文件不在Git管理范围，容易丢失
- 缺失时Spring Boot默默崩溃，无明确错误提示

**新方案（Spring Boot标准做法）**：

| 文件 | 是否提交Git | 内容 |
|---|---|---|
| [application.yml](file:///d:/JAVA/xiaxiang/src/main/resources/application.yml) | ✅ 提交 | 全部业务数据+admin账号，密钥用`${COS_SECRET_ID:}`环境变量占位符 |
| [application-local.yml](file:///d:/JAVA/xiaxiang/src/main/resources/application-local.yml) | ❌ 忽略 | 本地密钥+`app.env=dev`（禁止写操作） |

**三层保险机制**：
1. **spring.config.import**：`application.yml`中`spring.config.import: optional:classpath:application-local.yml`，本地自动加载密钥，服务器不存在也不报错
2. **CosConfig启动检查**：[CosConfig.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/config/CosConfig.java#L37-L88) 启动时强制校验密钥，缺失时打印4种可能原因+解决方案，立即FAIL FAST
3. **.gitignore精确控制**：只忽略`application-local.yml`，`application.yml`正常提交

**涉及文件**：
- [application.yml](file:///d:/JAVA/xiaxiang/src/main/resources/application.yml)（重构为含全部数据+环境变量密钥）
- [application-local.yml](file:///d:/JAVA/xiaxiang/src/main/resources/application-local.yml)（新建，本地密钥）
- [.gitignore](file:///d:/JAVA/xiaxiang/.gitignore#L25-L30)（修改，只忽略local文件）
- [CosConfig.java](file:///d:/JAVA/xiaxiang/src/main/java/org/example/xiaxiang/config/CosConfig.java)（增加启动检查器）

### 10.3 服务器部署流程

```bash
# 1. 拉取代码（application.yml自动更新）
git pull

# 2. 编译打包
mvn clean package -DskipTests

# 3. 设置环境变量
export COS_SECRET_ID="你的SecretId"
export COS_SECRET_KEY="你的SecretKey"

# 4. 启动
java -jar target/xiaxiang-building-tour.jar
```

此方案**完全替代**之前的部署流程，不再需要手动scp上传application.yml。

---

## 十一、项目约束更新（2026-08-03）

### 硬约束（新增）
1. `application.yml`必须提交到Git（含全部业务数据，密钥用环境变量）
2. `application-local.yml`被.gitignore忽略（本地密钥专用）
3. CosConfig启动时必须校验密钥，缺失时FAIL FAST并打印诊断信息
4. 所有素材类型（图片/视频/3D模型/音频）未绑定时必须显示Slot编号占位符

### 工程规范（更新）
1. 3D模型加载三级fallback改为：3D模型→部位图片→**DOM Slot占位符**（不再用示例立方体）
2. 所有列表页/详情页禁止使用AI生成图作为fallback
3. 服务器部署通过环境变量注入密钥，不再依赖手动上传配置文件
