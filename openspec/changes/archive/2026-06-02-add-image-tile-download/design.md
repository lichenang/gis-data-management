## Context

当前系统已实现原始 GeoTIFF 的预签名 URL 下载（`GET /api/v1/images/{id}/download-url`），以及通过 GeoWebCache 自动 seed 切片缓存（zoom 0-18，`image/png`）。但缺少将 GWC 已缓存切片打包为 ZIP 供用户下载的能力。

GWC 切片存储在 `{geoserver.data-dir}/gwc/gisplatform_raster_{id}/{z}/{x}/{y}.png`，遵循 TMS 目录结构。后端需要直接读取该目录打包，或通过 HTTP 代理瓦片后打包。目前系统部署为单体模式，后端与 GeoServer 共享文件系统，因此直接读取是可行的。

## Goals / Non-Goals

**Goals:**
- 新增 `POST /api/v1/images/{id}/tile-package` 接口，支持 zoom 范围参数
- 通过 `ZipOutputStream` 流式打包 GWC 瓦片目录，O(1) 内存
- 实现瓦片数量估算与硬上限校验（默认 100,000），防止请求超限
- 前端操作列新增"下载切片包"按钮，支持 zoom 范围选择
- 新增 `geoserver.data-dir` 配置项，指向 GeoServer data_dir

**Non-Goals:**
- 不实现 WMTS HTTP 代理降级（策略 B）
- 不实现异步生成 + 预签名 URL
- 不实现断点续传
- 不实现并发请求限制

## Decisions

### 1. 数据平面：直接文件系统读取（策略 A）而非 WMTS HTTP 代理

| 维度 | 策略 A（文件系统） | 策略 B（HTTP 代理） |
|------|------------------|-------------------|
| 性能 | 零网络开销，磁盘顺序读 | 每个瓦片一次 HTTP 请求 |
| 内存 | O(1)，每瓦片读入后立即写入 ZIP | 同左 |
| 前提 | 需 `geoserver.data-dir` 配置 | 需 GeoServer HTTP 可达 |
| 复杂度 | 低（文件夹遍历 + ZipOutputStream） | 中（并发下载 + 错误重试） |

**结论**：选择策略 A。部署拓扑为单体同主机，FS 访问可行且性能最优。

### 2. 响应方式：同步流式 ZIP 而非异步生成

同步流式直接通过 `HttpServletResponse.getOutputStream()` → `ZipOutputStream` 写入，用户请求后立即开始下载。相比异步方案（先存 MinIO 再生成 presigned URL），实现简单且无临时文件清理负担。

### 3. 下载触发：axios blob 而非 window.open

POST 请求携带 JSON body（zoom 参数），无法用 `window.open` 触发。浏览器通过 `axios({ responseType: 'blob' })` 接收二进制流，创建 `Blob` URL 后模拟 `<a>` 点击下载。

### 4. 默认 zoom 上限 14 而非 18

| Zoom 范围 | 典型瓦片数 | 典型 ZIP 体积 |
|-----------|-----------|-------------|
| 0-14 | ~20K | ~100-1000 MB |
| 0-16 | ~85K | ~500-4000 MB |
| 0-18 | ~340K | ~2-16 GB |

默认 0-14 可在绝大多数情况下满足用途（14 级切片已有足够分辨率），同时将体积控制在合理范围。用户可通过对话框手动调高。

### 5. 配置放在 tile-package 前缀而非 geoserver 下

`tile-package.max-tiles` 和 `tile-package.default-zoom-stop` 与下载能力相关，而非 GeoServer 连接。`geoserver.data-dir` 属于 GeoServer 基础配置，保留在 `geoserver` 前缀下，与其他 `geoserver.*` 配置保持语义一致。

## Risks / Trade-offs

| 风险 | 影响 | 缓解 |
|------|------|------|
| GWC 目录在打包过程中被 GWC seed 任务并发写入 | 打包到不完整切片 | 检测 `cacheSeedStatus=seeded` 才允许打包；打包过程中新增/修改的文件会被遗漏，但每个瓦片独立无所谓 |
| ZIP 包未设置 `Content-Length` 导致浏览器无进度条 | 用户体验差 | 可在响应头加 `X-Estimated-Size` 前端读取后显示预估进度 |
| 超大 ZIP 导致浏览器下载时 Blob URL OOM | 浏览器崩溃 | 瓦片数量硬上限（100K）是第一道防线 |
| 打包请求与 Tiling 任务同时使用 IO 造成竞争 | 打包速度下降 | 非关键路径问题，暂不处理 |
| `geoserver.data-dir` 未配置 | 接口返回 500 | 启动时校验配置，缺失则禁用该接口并打 WARN 日志 |
