# 薪火侨乡 - 侨乡建筑数字化云游平台 开发日志

> 记录时间：2026-08-08
> 项目路径：`d:/JAVA/xiaxiang`
> 技术栈：Java 17 + Spring Boot 2.7 + Thymeleaf + MySQL + 腾讯云 COS
> 服务器：Ubuntu / Nginx / Java 17
> 部署端口：8080

---

## 一、本次会话核心任务

### 首页导览模式功能实现

为首页添加"导览模式"切换功能：
- 首页默认为**内容展示模式**（显示实际建筑图片、故事详情、互动选项等）
- 点击"🗂️ 导览"按钮切换为**导览模式**（纯入口卡片导航，不展示实际内容）
- 导览卡片正确跳转到对应二级页面（`/map`、`/quiz`、`/location/{id}`）
- 切换状态通过 `localStorage` 持久化
- 浅色/深色主题切换时导览卡片样式自动适配

---

## 二、Bug 修复与优化

### Bug 1：主题切换功能失效

| 项目 | 说明 |
|------|------|
| **问题描述** | 点击主题切换按钮（🌙/☀️）无反应，页面不切换深浅色主题 |
| **根因分析** | `index.html` 内联脚本和外部 `theme-switch.js` 都定义了 `toggleTheme()` 函数，造成**函数覆盖冲突**。内联脚本先加载，外部脚本后加载覆盖了内联版本，但两个版本实现不同导致逻辑混乱 |
| **修复方案** | 删除 `index.html` 中重复的 `toggleTheme()` 定义，统一使用外部 `theme-switch.js` 中的版本。拆分初始化函数为 `initPortalMode()`，避免与 `theme-switch.js` 中的 `initTheme()` 冲突 |
| **涉及文件** | [index.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/index.html#L668-L717) |

### Bug 2：导览卡片样式丢失

| 项目 | 说明 |
|------|------|
| **问题描述** | 导览卡片变成丑陋的文字链接列表（蓝色文字+下划线），完全丢失卡片样式 |
| **根因分析** | 将卡片容器从 `<div>` 改为 `<a>` 标签实现直接跳转后，浏览器默认链接样式（`color: blue; text-decoration: underline`）覆盖了 CSS 中定义的卡片样式。CSS 特异性不足，无法覆盖浏览器 UA 样式 |
| **修复方案** | 在 `.portal-entry-card` 及其子元素的所有样式属性添加 `!important` 声明，强制覆盖 `<a>` 标签默认样式。添加 `:link`、`:visited` 伪类选择器确保链接状态一致性 |
| **涉及文件** | [qiaoyun.css](file:///d:/JAVA/xiaxiang/src/main/resources/static/css/qiaoyun.css#L4736-L4865) |

### Bug 3：服务器部署后静态资源缓存问题

| 项目 | 说明 |
|------|------|
| **问题描述** | 本地开发环境样式正常，但部署到服务器后导览卡片样式丢失，浏览器缓存旧版 CSS |
| **根因分析** | Spring Boot 默认启用静态资源缓存，且 HTML 中引用的 CSS/JS 文件没有版本号参数，浏览器使用本地缓存的旧版文件 |
| **修复方案** | 1. 在 HTML 中为所有 CSS/JS 引用添加版本号参数 `?v=20260808`，强制浏览器重新下载<br>2. 在 `application.yml` 中配置 `spring.web.resources.cache.period: 0` 禁用服务器端缓存 |
| **涉及文件** | [index.html](file:///d:/JAVA/xiaxiang/src/main/resources/templates/index.html#L7)、[application.yml](file:///d:/JAVA/xiaxiang/src/main/resources/application.yml#L23-L27) |

---

## 三、功能实现详情

### 首页导览模式

**切换机制**：
- 通过 `body[data-portal-mode="true"]` 属性控制内容/导览模式显示
- 实际内容模式：三大区块（云游宝源坊、互动体验、建筑故事）显示实际内容
- 导览模式：三大区块显示纯入口卡片（图标+标题+简述）

**导览卡片结构**：
```html
<a class="portal-entry-card primary" href="/map">
    <div class="portal-card-icon">🏛️</div>
    <h3 class="portal-card-title">宝源坊3D全景</h3>
    <p class="portal-card-desc">沉浸式3D云游 · 古村空间布局</p>
</a>
```

**CSS 控制**：
```css
.portal-container { display: none; }  /* 默认隐藏导览卡片 */
body[data-portal-mode="true"] .portal-container { display: block; }
body[data-portal-mode="true"] .content-container { display: none; }
```

**入口卡片跳转映射**：

| 区块 | 卡片 | 跳转路径 |
|------|------|----------|
| 云游宝源坊 | 宝源坊3D全景（主） | `/map` |
| | 6栋特色建筑 | `/map` |
| | 建筑故事集 | `/stories` |
| 互动体验 | 商贸之家（主） | `/quiz` |
| | 书香之家 | `/quiz` |
| | 普通人家 | `/quiz` |
| 建筑故事 | 宝源坊碉楼（主） | `/location/1` |
| | 方形古井 | `/location/2` |
| | 青石板街 | `/location/3` |

---

## 四、涉及文件清单

| 文件路径 | 修改内容 |
|----------|----------|
| `src/main/resources/templates/index.html` | 删除重复的 `toggleTheme()`，添加 `initPortalMode()`，CSS/JS 添加版本号参数，导览卡片改为 `<a>` 标签 |
| `src/main/resources/static/css/qiaoyun.css` | 新增导览卡片样式（`.portal-entry-card` 等），添加 `!important` 覆盖浏览器默认样式，支持浅色主题适配 |
| `src/main/resources/application.yml` | 添加 `spring.web.resources.cache.period: 0` 禁用静态资源缓存 |

---

## 五、部署注意事项

1. **打包命令**：`mvn clean package -DskipTests`（必须 `clean`，避免旧文件残留）
2. **上传路径**：`/opt/xiaxiang/`
3. **重启命令**：
   ```bash
   kill $(cat /opt/xiaxiang/app.pid) 2>/dev/null || fuser -k 8080/tcp
   cd /opt/xiaxiang
   nohup java -jar xiaxiang-building-tour-1.0-SNAPSHOT.jar > app.log 2>&1 &
   echo $! > app.pid
   ```
4. **浏览器缓存**：部署后使用 `Ctrl+Shift+R` 强制刷新
5. **Nginx 缓存**：如仍有样式问题，检查 Nginx 配置中是否有缓存规则

---

## 六、项目约束与规范

- **后端管理**：后台管理部分不得修改
- **3D 模型加载**：三级回退策略（.splat 文件 → 图片 → DOM 占位符）
- **COS 存储**：material filling 状态必须严格反映 COS bucket 实际内容
- **端口规范**：应用必须使用 8080 端口
- **部署流程**：Maven package → 上传服务器 → 杀掉旧进程 → 启动新进程

---

## 七、待办事项

- [ ] 验证导览卡片在移动端的响应式布局（<900px 单列显示）
- [ ] 确认所有二级页面跳转功能正常（`/stories`、`/location/{id}` 等）
- [ ] 考虑在 Nginx 层也禁用静态资源缓存（如需）
