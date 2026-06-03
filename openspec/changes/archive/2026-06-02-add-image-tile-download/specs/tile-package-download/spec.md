## 模块划分

| 模块 | 职责 | 关键类 / 文件 |
|------|------|---------------|
| 配置层 | 管理 GeoServer data_dir 路径和切片包下载参数 | `GeoServerProperties`（修改），`application.yml`（修改） |
| 接口层 | 接收前端请求，校验参数，返回 ZIP 流或错误 | `TilePackageController` |
| 业务层 | 切片包打包核心逻辑：目录扫描、文件读取、流式写入 ZIP | `TilePackageService` / `TilePackageServiceImpl` |
| DTO 层 | 请求参数对象 | `TilePackageRequest` |
| 前端 API | 封装后端接口调用 | `api/image.ts`（修改） |
| 前端视图 | 操作列按钮、zoom 范围选择对话框、下载触发 | `views/images/index.vue`（修改） |

## 数据流设计

```
用户点击"下载切片包"
        │
        ▼
Zoom 范围选择对话框 (默认 0-14)
        │
        ▼
axios.post("/api/v1/images/{id}/tile-package", { zoomStart, zoomStop }, { responseType: "blob" })
        │
        ▼
TilePackageController.packageTiles(id, request)
        │
        ├─ 校验 Dataset 存在且 status=published
        ├─ 校验 cacheSeedStatus=seeded
        ├─ 估算瓦片数量 → 超限则拒绝
        │
        ▼
TilePackageServiceImpl.packageTiles(id, request, outputStream)
        │
        ├─ 解析 GWC 目录路径: {dataDir}/gwc/gisplatform_raster_{id}/
        ├─ 遍历 zoom 范围: for (z = zoomStart; z <= zoomStop; z++)
        │       for (x = xMin; x <= xMax; x++)
        │           for (y = yMin; y <= yMax; y++)
        │               if (tile.exists) ZipOutputStream 写入
        │
        ▼
浏览器接收 ZIP 二进制流 → Blob URL → 触发下载
```

## 接口列表

| 接口 | 方法 | 请求体 | 响应 | 说明 |
|------|------|--------|------|------|
| `/api/v1/images/{id}/tile-package` | POST | `TilePackageRequest` | `application/zip` 流 | 生成并下载切片包 |
| `/api/v1/images/tile-config` | GET | - | `{ maxTiles, defaultZoomStop }` | 获取前端配置（可选） |

## ADDED Requirements

### Requirement: 后端支持流式打包 GWC 切片为 ZIP

后端 SHALL 支持将指定影像图层的 GWC 缓存切片打包为 ZIP 文件流式返回。

#### Scenario: 成功打包指定 zoom 范围的切片并下载
- **WHEN** 前端 POST `/api/v1/images/{id}/tile-package` 请求，`zoomStart=0, zoomStop=10`
- **THEN** 后端返回 `Content-Type: application/zip`，ZIP 内含 `0/0/0.png` 至 `10/{x}/{y}.png` 的瓦片文件

#### Scenario: 瓦片数量超过硬上限被拒绝
- **WHEN** 请求的 zoom 范围导致预估瓦片数 > `tile-package.max-tiles`（默认 100,000）
- **THEN** 后端返回 `400`，消息 `瓦片数量超出限制（预估 {n}，上限 {max}）`

#### Scenario: 数据集未发布时拒绝打包
- **WHEN** POST 请求的数据集 `status != 'published'`
- **THEN** 后端返回 `400`，消息 `影像未发布，尚无切片缓存`

#### Scenario: 切片状态非 seeded 时拒绝打包
- **WHEN** POST 请求的数据集 `cacheSeedStatus != 'seeded'`
- **THEN** 后端返回 `400`，消息 `切片尚未完成，当前进度 {progress}%`

#### Scenario: GWC 目录不存在返回 500
- **WHEN** GWC 目录 `{dataDir}/gwc/gisplatform_raster_{id}/` 不存在
- **THEN** 后端返回 `500`，消息 `切片缓存目录不存在`

#### Scenario: ZipOutputStream 流式写入不 OOM
- **WHEN** 打包包含 50,000 个瓦片
- **THEN** 每个瓦片通过 `FileInputStream.transferTo()` 逐块写入，ZIP 不缓冲到内存

### Requirement: 前端提供切片包下载按钮

前端 SHALL 在影像管理页面的操作列提供"下载切片包"按钮，并支持 zoom 范围选择。

#### Scenario: 操作列显示下载切片包按钮
- **WHEN** 影像列表加载完成，任意行处于 `published` 状态
- **THEN** 该行操作列显示"下载切片包"按钮

#### Scenario: 点击按钮弹出 zoom 范围选择对话框
- **WHEN** 用户点击"下载切片包"按钮
- **THEN** 弹出对话框，显示 zoomStart (0) 和 zoomStop (14) 的输入框，以及预估瓦片数提示

#### Scenario: 确认后触发浏览器下载
- **WHEN** 用户在对话框中确认下载
- **THEN** 前端调用 `downloadTilePackage()`，浏览器下载 ZIP 文件，文件名为 `{影像名称}_tiles_z{start}-z{stop}.zip`

#### Scenario: 下载失败显示错误提示
- **WHEN** 后端返回错误或网络异常
- **THEN** 前端弹出 `ElMessage.error` 提示错误信息

### Requirement: 后端校验 geoserver.data-dir 配置

后端 SHALL 在启动时检查 `geoserver.data-dir` 是否配置，未配置时禁用切片包下载接口并打印 WARN 日志。

#### Scenario: data-dir 未配置时返回配置错误
- **WHEN** `geoserver.data-dir` 未设置
- **THEN** POST `/api/v1/images/{id}/tile-package` 返回 `500`
