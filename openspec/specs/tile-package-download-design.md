# 影像切片包下载设计方案

## 1. 目标

在已实现原始 GeoTIFF 下载的基础上（`GET /api/v1/images/{id}/download-url`），新增**切片包下载**功能，允许用户将 GeoWebCache 已缓存的 PNG 切片打包为 ZIP 下载，用于离线使用、第三方系统集成等场景。

## 2. 现状分析

### 2.1 已存在的能力

| 能力 | 状态 | 说明 |
|------|------|------|
| 原始 GeoTIFF 下载 | ✅ 已实现 | MinIO presigned URL |
| GWC 切片缓存 | ✅ 已实现 | 发布时自动 seed，zoom 0-18 |
| GWC 切片状态轮询 | ✅ 已实现 | `TileSeedService` 定时查询 GWC REST API |
| GeoServer data_dir 可访问性 | ❓ 未知 | 需新增配置 |
| GWC REST tilepackage API | ❓ 未知 | 需验证 GeoServer 2.26.x 支持情况 |

### 2.2 GWC 磁盘目录结构

```
{data_dir}/gwc/
  ├── gisplatform_raster_27/          # {workspace}_{layerName}/
  │   ├── 0/                          # z 层级
  │   │   ├── 0/                      # x 列
  │   │   │   └── 0.png               # y 行
  │   │   ├── 1/
  │   │   │   └── 0.png
  │   ├── 1/
  │   │   ├── 0/
  │   │   │   ├── 0.png
  │   │   │   └── 1.png
  │   │   └── 1/
  │   └── ...
  └── gisplatform_raster_28/
```

层标识转换规则：GWC 内部使用 `{workspace}:{layerName}` 标识层，但在磁盘上以 `{workspace}_{layerName}` 作为目录名（将 `:` 替换为 `_`）。

### 2.3 瓦片数量估算

对于一个实际地理范围（非全球）的影像，各 zoom 级别的瓦片数大致为：

| Zoom | 瓦片数（典型值） | 累计 |
|------|-----------------|------|
| 0-10 | ~1K | ~1K |
| 0-12 | ~4K | ~5K |
| 0-14 | ~16K | ~21K |
| 0-16 | ~64K | ~85K |
| 0-18 | ~256K | ~340K |

每个 PNG 瓦片约 5-50KB，上述范围对应 ZIP 体积：

| Zoom 范围 | 瓦片数 | 预估 ZIP 体积 |
|-----------|--------|--------------|
| 0-14 | ~21K | ~100-1000 MB |
| 0-16 | ~85K | ~500-4000 MB |
| 0-18 | ~340K | ~2-16 GB |

---

## 3. 架构方案

### 3.1 核心决策：数据平面路径

后端需要读取 GWC 瓦片文件。有三种可能的部署拓扑：

```
┌───────────────── Deployment Topologies ─────────────────┐
│                                                          │
│  A) 单体部署                      B) 容器分离             │
│  ┌──────────────────┐            ┌────────┐ ┌────────┐  │
│  │  Backend App     │            │Backend │ │GeoSrv  │  │
│  │  GeoServer(embed)│            │        │ │        │  │
│  │                  │            │  NFS   │ │  NFS   │  │
│  │  data_dir/gwc/ ◀─┼──────┐     │  share │ │  share │  │
│  └──────────────────┘      │     └────┬───┘ └────┬───┘  │
│                             │          │          │      │
│                      C) 完全隔离       └──────────┘      │
│                      ┌────────┐ ┌────────┐               │
│                      │Backend │ │GeoSrv  │               │
│                      │        │ │        │               │
│                      │ no FS  │ │data_dir│               │
│                      │ access │ │  only  │               │
│                      └────────┘ └────────┘               │
└──────────────────────────────────────────────────────────┘
```

**方案选型**：

| 部署场景 | 数据平面 | 推荐策略 |
|---------|---------|---------|
| A: 单体/同主机 | 直接 FS 读取 | **策略 A（直接文件打包）** |
| B: 共享卷 | NFS 共享 | **策略 A（直接文件打包）** |
| C: 完全隔离 | 无 FS 共享 | **策略 B（HTTP 代理瓦片）** |

### 3.2 策略 A：直接文件系统打包（推荐，默认实现）

```
┌──────────┐   POST /api/v1/images/{id}/tile-package   ┌───────────────┐
│ Frontend │ ─────────────────────────────────────────▶ │   Backend     │
│          │   { zoomStart, zoomStop, bounds }          │               │
│          │                                            │   TilePackage │
│          │   流式 ZIP 下载 (Content-Type: zip)         │   Service    │
│          │ ◀───────────────────────────────────────── │               │
└──────────┘                                            └───────┬───────┘
                                                                │
                                                    读取 GWC 目录
                                                    ZipOutputStream
                                                    流式写入
                                                                │
                                                                ▼
                                                        data_dir/gwc/
                                                    gisplatform_raster_{id}/
```

**优点**：
- 直接读取文件系统，零网络开销
- `ZipOutputStream` 流式写入，O(1) 内存
- 实现简单，复用 `ExportUtil.zipFiles` 模式

**前提**：
- 后端可访问 GeoServer 的 `data_dir/gwc/` 目录
- 需新增配置项 `geoserver.data-dir`（可选，也可硬编码约定路径）

### 3.3 策略 B：WMTS HTTP 代理瓦片（降级方案）

```
┌──────────┐   POST /api/v1/images/{id}/tile-package   ┌───────────────┐
│ Frontend │ ─────────────────────────────────────────▶ │   Backend     │
│          │                                            │               │
│          │   流式 ZIP 下载                             │   TilePackage │
│          │ ◀───────────────────────────────────────── │   Service     │
└──────────┘                                            └───────┬───────┘
                                                                │
                                         对每个所需瓦片发起 HTTP 请求
                                                         │
                                         ┌─────────────────┼────────────┐
                                         ▼                 ▼            ▼
                                   GET /gwc/service/     ...          ...
                                   wmts/{layer}/{z}/{x}
                                   /{y}.png
                                   │
                                   ▼
                              GeoServer / GWC
```

**优点**：
- 无需 FS 访问，适用于容器/远程部署
- 利用 GWC 已有的 HTTP 缓存服务

**缺点**：
- 每个瓦片一个 HTTP 请求，大量请求时性能差
- 可考虑 `Connection: keep-alive` + 并发下载优化
- 瓦片较多时打包时间显著增长

---

## 4. API 设计

### 4.1 接口定义

```
POST /api/v1/images/{id}/tile-package

Request Body (JSON):
{
  "zoomStart": 0,           // 可选，起始缩放级别，默认 0
  "zoomStop": 14,           // 可选，结束缩放级别，默认 14
  "bounds": {               // 可选，地理范围，默认从 dataset.extent 获取
    "minX": 120.0,
    "minY": 30.0,
    "maxX": 121.0,
    "maxY": 31.0
  }
}

Response (成功，直接流式返回 ZIP):
  Status: 200
  Content-Type: application/zip
  Content-Disposition: attachment; filename="影像名称_tiles_z0-z14.zip"

Response (参数校验失败):
  Status: 400
  {
    "code": 400,
    "message": "瓦片数量超出限制 (预估 50000，上限 100000)",
    "data": null
  }

Response (数据集不存在或未发布):
  Status: 404
  {
    "code": 404,
    "message": "影像数据集不存在或未发布",
    "data": null
  }
```

### 4.2 参数验证规则

| 参数 | 验证 | 默认值 |
|------|------|--------|
| `zoomStart` | 0 ≤ zoomStart ≤ zoomStop ≤ 18 | 0 |
| `zoomStop` | 0 ≤ zoomStart ≤ zoomStop ≤ 18 | 14（避免默认导出过大） |
| `bounds` | 有效 WGS84 经纬度范围 | `dataset.extent`（JSONB 字段） |

### 4.3 瓦片数量上限

```java
// 瓦片数量估算公式
long estimateTileCount(int zoomStart, int zoomStop, Bounds bounds) {
  long total = 0;
  for (int z = zoomStart; z <= zoomStop; z++) {
    int xMin = (int) Math.floor((bounds.minX + 180) / 360 * (1 << z));
    int xMax = (int) Math.floor((bounds.maxX + 180) / 360 * (1 << z));
    double latRadMin = Math.toRadians(bounds.minY);
    double latRadMax = Math.toRadians(bounds.maxY);
    int yMin = (int) Math.floor((1 - Math.log(Math.tan(latRadMin) + 1 / Math.cos(latRadMin)) / Math.PI) / 2 * (1 << z));
    int yMax = (int) Math.floor((1 - Math.log(Math.tan(latRadMax) + 1 / Math.cos(latRadMax)) / Math.PI) / 2 * (1 << z));
    total += (long) (xMax - xMin + 1) * (yMax - yMin + 1);
  }
  return total;
}
```

- **硬上限**: 100,000 个瓦片（可配置）
- 超过上限时，拒绝请求并提示调整 zoom 范围

---

## 5. 后端实现方案

### 5.1 新增文件

| 文件 | 用途 |
|------|------|
| `dto/TilePackageRequest.java` | 请求体 DTO |
| `service/TilePackageService.java` | 接口定义 |
| `service/impl/TilePackageServiceImpl.java` | 接口实现 |
| `controller/TilePackageController.java` | REST 接口控制器 |

### 5.2 核心流程

```
TilePackageController.packageTiles(id, request)
  │
  ├─ 1. 校验 Dataset 存在、type=raster、status=published
  │
  ├─ 2. 解析请求参数（zoomStart, zoomStop, bounds）
  │
  ├─ 3. 估算瓦片数量 → 超过上限则拒绝
  │
  ├─ 4. 根据策略选择数据平面:
  │      ┌─ 策略 A: 读取 GWC 本地目录
  │      └─ 策略 B: HTTP 代理瓦片（降级）
  │
  ├─ 5. 设置 HTTP 响应头:
  │      Content-Type: application/zip
  │      Content-Disposition: attachment; filename="..."
  │      X-Tile-Count: 12345
  │      X-Estimated-Size: 500MB
  │
  └─ 6. 流式写入 ZipOutputStream:
         for (z = zoomStart; z <= zoomStop; z++) {
           for (x = xMin; x <= xMax; x++) {
             for (y = yMin; y <= yMax; y++) {
               File tile = new File(gwcDir, z + "/" + x + "/" + y + ".png");
               if (tile.exists()) {
                 zos.putNextEntry(new ZipEntry(z + "/" + x + "/" + y + ".png"));
                 FileInputStream fis → fis.transferTo(zos);
                 zos.closeEntry();
               }
             }
           }
         }
```

### 5.3 大文件处理（OOM 防护）

```
┌─────────────────────────────────────────────────────────────┐
│                   内存安全策略                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. ZipOutputStream 流式写入                                  │
│     - 一次只读一个瓦片到内存（FileInputStream）                │
│     - 单个瓦片最大 ~50KB，内存安全                            │
│     - 不缓冲整个 ZIP 到内存                                  │
│                                                             │
│  2. 瓦片数量硬上限（默认 100K）                               │
│     - 防止意外创建超大 ZIP                                    │
│     - 可配置，超限时提示用户缩小范围                           │
│                                                             │
│  3. 请求超时保护                                              │
│     - 后端配置合理的请求超时（如 5 分钟）                      │
│     - 前端在超时 / 流中断时给出明确提示                        │
│                                                             │
│  4. 不依赖 tmp 文件                                           │
│     - 直接 `response.getOutputStream()` → `ZipOutputStream`   │
│     - 零磁盘 I/O（除读取瓦片外）                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 5.4 配置项

```yaml
# application.yml 新增
tile-package:
  # 数据平面策略: filesystem / http-proxy
  strategy: filesystem
  # 瓦片数量硬上限
  max-tiles: 100000
  # 默认 zoom 上限（避免用户误操作导出全量）
  default-zoom-stop: 14
  # 策略 A 需要: GeoServer data_dir 路径
  # data-dir: /opt/geoserver/data_dir
```

```yaml
# 或复用 geoserver 配置
geoserver:
  data-dir: ${GEOSERVER_DATA_DIR:/opt/geoserver/data_dir}
```

> **关于 data_dir 配置**：当前 `GeoServerProperties` 没有 `dataDir` 字段。建议新增该字段，与现有 `url`、`workspace` 并列。如果未配置，策略 A 不可用，自动降级到策略 B。

### 5.5 异常处理

| 异常场景 | HTTP 状态码 | 消息 |
|---------|------------|------|
| 数据集不存在 | 404 | `影像数据集不存在` |
| 数据集未发布 | 400 | `影像未发布，尚无切片缓存` |
| 切片未完成 | 400 | `切片尚未完成，当前进度 {progress}%` |
| 瓦片数超限 | 400 | `瓦片数量超出限制（预估 {n}，上限 {max}）` |
| GWC 目录不存在 | 500 | `切片缓存目录不存在，请确认切片已完成` |
| GWC 目录权限错误 | 500 | `无法读取切片缓存` |
| 流写入中断 | 500（部分写入） | `下载中断` |

---

## 6. 前端实现方案

### 6.1 新增按钮

在 `views/images/index.vue` 操作列新增"下载切片包"按钮，紧跟在"下载原始影像"之后：

```vue
<el-button type="primary" link @click="handleDownloadTiles(row)">
  下载切片包
</el-button>
```

### 6.2 下载对话框（可选增强）

可提供 zoom 范围选择对话框，让用户指定要下载的缩放级别：

```vue
<el-dialog v-model="tileDialog.visible" title="下载切片包" width="400px">
  <el-form>
    <el-form-item label="起始级别">
      <el-input-number v-model="tileDialog.zoomStart" :min="0" :max="18" />
    </el-form-item>
    <el-form-item label="结束级别">
      <el-input-number v-model="tileDialog.zoomStop" :min="0" :max="18" />
    </el-form-item>
    <el-form-item>
      <span class="text-gray-500">
        预估瓦片数: {{ estimatedTiles }}
      </span>
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="tileDialog.visible = false">取消</el-button>
    <el-button type="primary" @click="confirmDownloadTiles">下载</el-button>
  </template>
</el-dialog>
```

### 6.3 下载触发方式

**方式一：流式下载（推荐）**

```typescript
const handleDownloadTiles = async (row: ImageDataset) => {
  // 可选: 弹出 zoom 范围选择对话框
  // 直接调用接口，浏览器处理流式下载
  const response = await axios.post(
    `/api/v1/images/${row.id}/tile-package`,
    { zoomStart: 0, zoomStop: 14 },
    { responseType: 'blob' }
  )
  const url = URL.createObjectURL(new Blob([response.data]))
  const link = document.createElement('a')
  link.href = url
  link.download = `${row.name}_tiles_z0-z14.zip`
  link.click()
  URL.revokeObjectURL(url)
}
```

**方式二：直接 `window.open`（不推荐）**

因 POST 请求携带 body，无法用 `window.open` 直接触发。需使用方式一的 axios blob 方式。

### 6.4 API 函数

```typescript
// api/image.ts
export interface TilePackageRequest {
  zoomStart?: number   // 默认 0
  zoomStop?: number    // 默认 14
  bounds?: {
    minX: number
    minY: number
    maxX: number
    maxY: number
  }
}

export function downloadTilePackage(id: number, params: TilePackageRequest) {
  return instance.post(`/images/${id}/tile-package`, params, {
    responseType: 'blob',
    timeout: 300000  // 5 分钟超时
  })
}
```

---

## 7. 性能与风险评估

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|---------|
| 超大 ZIP 导致浏览器 OOM | 高 | 低 | 瓦片数量硬上限 + 前端估算提示 |
| 打包耗时过长导致超时 | 中 | 中 | 5 分钟请求超时 + 流式写入（边打包边下载） |
| GWC 目录不可访问 | 高 | 中 | 策略 B HTTP 代理降级 |
| 下载中断导致用户困惑 | 中 | 低 | 下载中显示进度提示（利用 `Content-Length` 传输大文件时） |
| 同时多个打包请求打满 IO | 中 | 低 | 可在后续加并发限制 |

---

## 8. 推荐实施路径

```
Phase 1 (P0): 基础功能
├── 新增配置项 geoserver.data-dir
├── 实现 TilePackageController + TilePackageService
├── 策略 A：直接文件系统打包（ZipOutputStream 流式）
├── 瓦片数量估算 + 硬上限
├── 前端按钮 + blob 下载
└── 验证：单张影像 0-14 zoom 打包

Phase 2 (P1): 体验优化
├── 前端 zoom 范围选择对话框
├── 预估瓦片数提示
├── 下载进度反馈
├── 文件名包含影像名称 + zoom 范围
└── Content-Length / 文件大小预估响应头

Phase 3 (P2): 健壮性
├── 策略 B：WMTS HTTP 代理降级
├── 并发限制（Semaphore）
├── 下载中断恢复（断点续传？—— 复杂度高，暂不引入）
└── 大文件异步打包 + 预签名 URL（如需支持 0-18 全量导出）
```

## 9. 关联文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/main/java/.../config/GeoServerProperties.java` | 修改 | 新增 `dataDir` 字段 |
| `backend/src/main/java/.../dto/TilePackageRequest.java` | 新增 | 请求 DTO |
| `backend/src/main/java/.../controller/TilePackageController.java` | 新增 | POST 接口 |
| `backend/src/main/java/.../service/TilePackageService.java` | 新增 | 接口 |
| `backend/src/main/java/.../service/impl/TilePackageServiceImpl.java` | 新增 | 实现（含策略 A/B） |
| `backend/src/main/resources/application.yml` | 修改 | 新增 tile-package 配置 |
| `frontend/src/api/image.ts` | 修改 | 新增 `downloadTilePackage` |
| `frontend/src/views/images/index.vue` | 修改 | 新增按钮 + 下载对话框 + 事件处理 |
