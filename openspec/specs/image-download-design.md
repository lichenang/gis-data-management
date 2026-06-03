# 影像数据下载设计方案

## 1. 影像数据存储现状诊断

### 1.1 MinIO 存储现状

| 维度 | 值 |
|------|-----|
| 存储桶 | `gis-raster`（配置项 `minio.bucket-raster`，默认 `${MINIO_BUCKET_RASTER:gis-raster}`） |
| 对象路径格式 | `images/{uuid}.tif`（例如 `images/550e8400-e29b-41d4-a716-446655440000.tif`） |
| 文件内容 | 原始 GeoTIFF 二进制，`content-type: image/tiff` |
| 文件大小 | 百万像素级 GeoTIFF，典型范围 10MB~500MB |
| MinIO 服务 | `http://localhost:9000`（配置项 `minio.endpoint`） |
| 当前使用的 API | `putObject`（写入）、`getObject`（读取流式下载） |
| 是否启用预签名 URL | 否 |

**上传流程**（`ImageServiceImpl.uploadImage`, 第 106~186 行）：

```
UploadController.upload()
  → ImageServiceImpl.uploadImage()
     1. bucketExists / makeBucket (若不存在)
     2. PutObjectArgs → minioClient.putObject()
         bucket: gis-raster
         object: images/{uuid}.tif
         contentType: image/tiff
     3. GeoTiffParser.parse() → RasterMetadata
     4. INSERT dataset (type=raster, storageType=minio, minioKey=images/{uuid}.tif)
     5. INSERT raster_metadata (关联 datasetId)
```

### 1.2 GeoServer 影像发布现状

**发布流程**（`ImageServiceImpl.publishImageDataset`, 第 216~303 行）：

```
PublishController.publish()
  → ImageServiceImpl.publishImageDataset(id)
     1. 从 raster_metadata 取 minioBucket + minioKey
     2. minioClient.getObject() → byte[] (整个文件读入内存)
     3. GeoServerCoverageStoreService.createImageMosaicStore()
        → PUT /rest/workspaces/gisplatform/coveragestores/
           raster_{id}/file.geotiff?configure=first&coverageName=raster_{id}
     4. 设置 Dataset.wmsUrl / Dataset.wmtsUrl
     5. 从 raster_metadata.transform 提取 extent
     6. tileSeedService.triggerSeed() → 异步切片
```

**GeoServer 端产生的文件**：

| 位置 | 用途 | 示例路径 |
|------|------|---------|
| `data_dir/data/gisplatform/raster_{id}/` | GeoServer 本地复制的 GeoTIFF | `data/gisplatform/raster_27/xxx.tif` |
| `data_dir/gwc/gisplatform_raster_{id}/` | GeoWebCache 瓦片缓存 | `gwc/gisplatform_raster_27/8/123/45.png` |

### 1.3 GeoWebCache 瓦片缓存现状

| 配置项 | 值 |
|--------|-----|
| GWC REST API | `POST /gwc/rest/seed/gisplatform:raster_{id}` |
| WMTS 服务 | `{geoserver_url}/gwc/service/wmts` |
| 切片格式 | `image/png` |
| 缩放级别 | 0~18 (配置项 `tilingMinZoom` / `tilingMaxZoom`) |
| 切片范围 | `-180,-90,180,90`（全局） |
| 并发线程 | 4 |

**GWC 目录结构惯例**：
```
gwc/
├── gisplatform_raster_27/
│   ├── 0/
│   │   ├── 0/
│   │   │   └── 0.png
│   ├── 1/
│   │   ├── 0/
│   │   │   └── 0.png
│   │   ├── 1/
│   │   │   └── 0.png
│   │   └── 0/
│   │       └── 1.png
│   └── ...
└── gisplatform_raster_28/
    └── ...
```

### 1.4 数据库字段记录

**Dataset 表 (raster 相关字段)**：

| 字段 | 类型 | 说明 | 取值示例 |
|------|------|------|---------|
| `type` | varchar | 数据集类型 | `"raster"` |
| `storage_type` | varchar | 存储类型 | `"minio"` |
| `minio_key` | varchar | MinIO 对象路径 | `"images/uuid.tif"` |
| `srs` | varchar | 坐标系 | `"EPSG:4326"` |
| `extent` | jsonb | 空间范围 | `{"minX":120,"minY":30,"maxX":121,"maxY":31}` |
| `wms_url` | varchar | WMS 服务地址 | `"http://localhost:8080/geoserver/wms"` |
| `wmts_url` | varchar | WMTS 服务地址 | `"http://localhost:8080/geoserver/gwc/service/wmts"` |
| `tile_status` | varchar | 切片状态 | `"pending"` / `"processing"` / `"completed"` / `"failed"` |
| `cache_seed_status` | varchar | 缓存状态 | `"idle"` / `"seeding"` / `"seeded"` |

**RasterMetadata 表**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `dataset_id` | bigint | 关联 Dataset |
| `file_name` | varchar | 原始文件名 |
| `file_size` | bigint | 文件大小（字节） |
| `minio_bucket` | varchar | MinIO 存储桶 |
| `minio_key` | varchar | MinIO 对象路径 |
| `width` | int | 影像宽度（像素） |
| `height` | int | 影像高度（像素） |
| `bands` | int | 波段数 |
| `pixel_type` | varchar | 像素类型 (`Float32`, `UInt16`, `Byte`) |
| `no_data_value` | double | 无效值 |
| `crs` | varchar | 坐标参考系统 |
| `transform` | jsonb | 仿射变换矩阵 / 包围盒 `{minX, minY, maxX, maxY}` |
| `overviews` | jsonb | 金字塔信息 `{resolutionX, resolutionY}` |
| `capture_time` | timestamp | 拍摄时间 |
| `validation_status` | varchar | 校验状态 |

---

## 2. 下载模式技术方案

### 2.1 下载原始影像（Presigned URL）

**方案**：后端生成 MinIO 预签名 URL，前端直接下载 GeoTIFF。

```
┌──────────┐    1. GET /api/v1/images/{id}/download-url    ┌──────────┐
│  Frontend │ ────────────────────────────────────────────▶ │  Backend │
│           │                                               │          │
│           │    2. 查询 raster_metadata 获取 minioKey      │          │
│           │    3. minioClient.getPresignedObjectUrl()     │          │
│           │    4. 返回 { downloadUrl, fileName }          │          │
│           │ ◀──────────────────────────────────────────── │          │
│           │                                               │          │
│           │    5. 直接 GET presignedUrl (浏览器下载)       │          │
│           │ ────────────────────────────────────────────▶ │  MinIO   │
│           │    6. 200 OK + GeoTIFF 文件流                │          │
│           │ ◀──────────────────────────────────────────── │          │
└──────────┘                                               └──────────┘
```

**关键代码路径**：

| 类 / 方法 | 位置 |
|-----------|------|
| `MinioClient.getPresignedObjectUrl()` | MinIO Java SDK 内置 |
| `PresignedGetObjectRequest` | 需构造 `GetObjectArgs` 参数 |
| 过期时间 | 建议 5 分钟（`Duration.ofMinutes(5)`） |
| 新 API | `GET /api/v1/images/{id}/download-url` → `R<DownloadUrlResponse>` |

**DownloadUrlResponse 结构**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "downloadUrl": "http://localhost:9000/gis-raster/images/uuid.tif?...",
    "fileName": "原始文件名.tif",
    "fileSize": 123456789,
    "expiresIn": 300
  }
}
```

**安全考虑**：
- Pre-signed URL 有效期短（5 分钟），过期后需重新申请
- 后端需校验用户是否有该数据集的访问权限
- 如需代理下载（不想暴露 MinIO 地址），可用现有 `ExportController.exportGeoTiff()` 的流式方案

**现有 ExportController 影响**：
- `exportGeoTiff()` 已实现从 MinIO 流式下载，可作为降级方案
- 新增的 presigned URL 方案作为"前端直连下载"优化

### 2.2 下载切片包（GWC Tile Package）

**方案 A：通过 GWC REST API 导出切片（推荐）**

GeoWebCache 提供 REST API `/gwc/rest/{layer}/tilepackage/{zoomStart}/{zoomStop}/{format}/{bounds}` 直接生成切片包。

```
Backend → GeoServer:
  GET /gwc/rest/gisplatform:raster_{id}/tilepackage/0/18/png/-180,-90,180,90

GeoServer 返回 → ZIP 流 (GWC 自动打包)
```

但此 API 在部分 GeoServer 版本中不可用。需要验证 GeoServer 2.26.x 的 GWC REST API 支持情况。

**方案 B：本地目录扫描打包（备选）**

如果 GWC API 不可用，直接从 GeoServer 的 `data_dir/gwc/` 目录读取瓦片文件打包。

```
┌──────────┐    1. GET /api/v1/images/{id}/tile-package     ┌──────────────┐
│  Frontend │ ────────────────────────────────────────────▶ │   Backend    │
│           │                                               │              │
│           │    2. 从 Dataset 获取 layerName (raster_{id}) │              │
│           │    3. 读取 GWC 缓存目录                       │              │
│           │       data_dir/gwc/gisplatform_raster_{id}/   │              │
│           │    4. 递归扫描所有 .png 文件                  │              │
│           │    5. 生成 ZIP 流（保持目录结构）             │              │
│           │       ├── tiles/0/0/0.png                     │              │
│           │       ├── tiles/1/0/0.png                     │              │
│           │       ├── ...                                 │              │
│           │    6. 返回 ZIP 下载                           │              │
│           │ ◀──────────────────────────────────────────── │              │
└──────────┘                                               └──────────────┘
```

**问题**：
- 后端需要知道 GeoServer 的物理路径（需新增配置项 `geoserver.data-dir`）
- 大型影像（zoom 0-18）可能有数万~数十万个瓦片，ZIP 体积巨大
- 打包时间长，可能超时

**方案 C：WMTS 客户端打包（前端实现）**

前端通过 WMTS 逐瓦片下载后在前端打包。

```
Frontend：
  for (z = 0; z <= 18; z++) {
    for (x = 0; x < 2^z; x++) {
      for (y = 0; y < 2^z; y++) {
        GET /gwc/service/wmts/gisplatform:raster_{id}/{z}/{x}/{y}.png
        → 添加到 JSZip
      }
    }
  }
  JSZip.generateAsync() → 下载
```

**问题**：瓦片数量呈指数级（2^18 × 2^18 ≈ 680 亿），完全不现实。

**推荐方案：有界范围的 WMTS 打包**

前端根据当前地图可视范围，只下载可见区域内的瓦片。

```
Frontend：
  1. 获取地图当前 extent + zoom 范围
  2. 计算可见瓦片集合
  3. 逐瓦片请求 WMTS → JSZip 打包 → 下载
  4. 限制：最多 5000 个瓦片，否则提示"区域过大"
```

**推荐实施路径**：

| 优先级 | 方案 | 复杂度 | 适用场景 |
|--------|------|--------|---------|
| P0 | 方案 C（前端有界打包） | 中 | 用户在地图上框选区域后下载 |
| P1 | 方案 A（GWC REST API） | 低（如果 API 可用） | 后台全量下载 |
| P2 | 方案 B（后端目录打包） | 中 | 方案 A 不可用时的降级 |

### 2.3 下载影像元数据（JSON Export）

**方案**：从 `raster_metadata` 表 + `dataset` 表组合返回结构化 JSON。

```
┌──────────┐    1. GET /api/v1/images/{id}/metadata        ┌──────────┐
│  Frontend │ ────────────────────────────────────────────▶ │  Backend │
│           │                                               │          │
│           │    2. SELECT raster_metadata WHERE dataset_id  │          │
│           │    3. SELECT dataset WHERE id                 │          │
│           │    4. 组合返回 JSON                           │          │
│           │ ◀──────────────────────────────────────────── │          │
└──────────┘                                               └──────────┘
```

**返回结构**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "dataset": {
      "id": 27,
      "name": "卫星影像_20240526",
      "type": "raster",
      "srs": "EPSG:4326",
      "extent": { "minX": 120.0, "minY": 30.0, "maxX": 121.0, "maxY": 31.0 },
      "status": "published",
      "wmsUrl": "http://localhost:8080/geoserver/wms",
      "wmtsUrl": "http://localhost:8080/geoserver/gwc/service/wmts",
      "tileStatus": "completed"
    },
    "rasterMetadata": {
      "fileName": "ortho_20240526.tif",
      "fileSize": 256000000,
      "width": 10000,
      "height": 8000,
      "bands": 3,
      "pixelType": "UInt16",
      "noDataValue": null,
      "crs": "EPSG:4326",
      "transform": {
        "minX": 120.0,
        "minY": 30.0,
        "maxX": 121.0,
        "maxY": 31.0
      },
      "overviews": {
        "resolutionX": 0.0001,
        "resolutionY": 0.000125
      },
      "validationStatus": "valid",
      "createTime": "2026-05-26T18:00:11"
    }
  }
}
```

---

## 3. 新增 API 接口一览

| 接口 | 方法 | 用途 | 下载模式 |
|------|------|------|---------|
| `/api/v1/images/{id}/download-url` | GET | 获取 MinIO 预签名直链（5 分钟有效） | 原始影像 |
| `/api/v1/images/{id}/download` | GET | 后端代理流式下载 GeoTIFF（已有 `exportGeoTiff`） | 原始影像（降级） |
| `/api/v1/images/{id}/tile-package` | POST | 生成并下载切片包（JSON body 指定 zoom 和 extent 范围） | 切片包 |
| `/api/v1/images/{id}/metadata` | GET | 下载影像元数据为 JSON 文件 | 元数据 |

## 4. 前端依赖

当前前端已安装：
- `geotiff@^2.1.3` — 可用于前端解析 GeoTIFF 元数据
- `ol` (OpenLayers) 依赖 `geotiff@^3.0.5` — WMTS 加载
- **需新增** `jszip` — 前端瓦片打包

---

## 5. 风险和注意事项

| 风险 | 缓解措施 |
|------|---------|
| MinIO 预签名 URL 泄露 | 有效期 5 分钟，配合用户权限校验 |
| GWC 瓦片包体积过大 | 前端限制范围 + zoom 级别，超过阈值提示用户 |
| GWC REST API 不同版本差异 | 先用方案 C（前端有界打包）作为默认，方案 A 作为可配置选项 |
| 后端流式下载大文件 OOM | 现有 `exportGeoTiff` 使用 `InputStream.transferTo()` 不会 OOM，presigned URL 更无此问题 |
| GeoServer data_dir 路径不可知 | 新增配置项 `geoserver.data-dir`（可选，仅方案 B 需要） |
