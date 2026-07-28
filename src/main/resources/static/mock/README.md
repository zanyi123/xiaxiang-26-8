# Mock 资源目录说明

本目录存放 Mock 模式下的占位资源文件。

## 使用方式

在 `application.yml` 中设置：
```yaml
app:
  mock-mode: true
```

开启后，系统会返回 `/mock/xxx` 路径的本地资源，而非真实 COS URL。

## 资源结构

```
mock/
├── cover-kaiping.jpg      # 开平碉楼封面图
├── cover-zili.jpg         # 自力村碉楼群封面图
├── cover-chikan.jpg       # 赤坎古镇骑楼封面图
├── models/
│   ├── kaiping.splat      # 开平碉楼 3D 模型（占位文件）
│   ├── zili.splat         # 自力村碉楼群 3D 模型（占位文件）
│   └── chikan.splat       # 赤坎古镇骑楼 3D 模型（占位文件）
└── videos/
    ├── kaiping-4k.mp4     # 开平碉楼 4K 视频（占位文件）
    ├── zili-4k.mp4        # 自力村碉楼群 4K 视频（占位文件）
    └── chikan-4k.mp4      # 赤坎古镇骑楼 4K 视频（占位文件）
```

## 注意事项

1. **占位文件大小**：真实 .splat 文件可能几百 MB 到几 GB，建议用 <10KB 的最小文件占位
2. **生产环境**：部署到服务器后，务必将 `mock-mode` 设为 `false`，使用真实 COS URL
3. **CORS 配置**：切换到真实 COS 后，必须确保 COS 存储桶的 CORS 规则已配置（允许 * 来源的 GET 请求）
