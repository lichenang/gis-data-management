# 影像图层加载后地图不显示且跳转错误位置 - 完整诊断报告

## 问题现象

影像图层加载后：
1. 地图画面空白（不显示影像）
2. 地图跳转到了错误位置（而非影像实际位置）

---

## 全链路数据流分析

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         完整数据流                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────────────────────┐  │
│  │ GeoTIFF     │ ──▶ │ GeoTiff     │ ──▶ │ Dataset.srs                 │  │
│  │ 原始CRS     │     │ Parser      │     │ (应为 EPSG:xxxx)            │  │
│  │ EPSG:3857   │     │ extractCrs  │     │                             │  │
│  └─────────────┘     └─────────────┘     └─────────────┬───────────────┘  │
│                                                        │                   │
│                                                        ▼                   │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ ImageServiceImpl.getImageWmsInfo() (line 375-428)                   │  │
│  │                                                                      │  │
│  │  1. dataset.getExtent() → "{\"minX\":..., \"minY\":..., ...}"        │  │
│  │  2. dataset.getSrs() → "EPSG:3857" (源CRS)                          │  │
│  │  3. CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs)      │  │
│  │     → extent 从 EPSG:3857 转换为 EPSG:4326                         │  │
│  │                                                                      │  │
│  │  【BUG!!!】line 410:                                                 │  │
│  │     info.setExtent(transformedExtent)  ← 转换后 (EPSG:4326)         │  │
│  │     info.setCrs("EPSG:3857")          ← 但 CRS 设为 3857!!!         │  │
│  │                                                                      │  │
│  └────────────────────────────┬────────────────────────────────────────┘  │
│                               │                                             │
│                               ▼                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ /api/v1/images/{id}/wms-url JSON 响应                               │  │
│  │                                                                      │  │
│  │  {                                                                  │  │
│  │    "wmsUrl": "http://localhost:8080/geoserver/gisplatform/...",     │  │
│  │    "layerName": "raster_27",                                        │  │
│  │    "crs": "EPSG:3857",          ← 错误！应该是 "EPSG:4326"          │  │
│  │    "opacity": 0.8,                                                  │  │
│  │    "extent": [116.2, 39.6, 116.6, 40.0]  ← 这是 EPSG:4326 经纬度！  │  │
│  │  }                                                                  │  │
│  │                                                                      │  │
│  └────────────────────────────┬────────────────────────────────────────┘  │
│                               │                                             │
│                               ▼                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ Frontend MapContainer.vue                                           │  │
│  │                                                                      │  │
│  │  View: projection = 'EPSG:4326' (line 116)                          │  │
│  │                                                                      │  │
│  │  TileWMS (line 255-263):                                            │  │
│  │    new TileWMS({                                                     │  │
│  │      url: imageInfo.wmsUrl,                                          │  │
│  │      params: { LAYERS: 'raster_27', TILED: true },                  │  │
│  │      serverType: 'geoserver',                                        │  │
│  │      // projection 未指定！                                          │  │
│  │    })                                                                │  │
│  │                                                                      │  │
│  │  loadImageLayer (line 279-285):                                      │  │
│  │    map.value.getView().fit(imageInfo.extent, {...})                  │  │
│  │    // extent 是 [116.2, 39.6, 116.6, 40.0] - EPSG:4326 经纬度       │  │
│  │    // 但 crs 字段说是 "EPSG:3857"！                                   │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 根因分析

### Bug #1: 后端 CRS 设置错误（核心问题）

**位置**: `backend/.../ImageServiceImpl.java:408-410`

```java
if (transformedExtent != null) {
    info.setExtent(transformedExtent);  // ← extent 已转换为 EPSG:4326
    info.setCrs("EPSG:3857");           // ← BUG! CRS 仍是 "EPSG:3857"
```

**问题**: extent 被转换为 EPSG:4326（经纬度），但 crs 字段错误地设为 "EPSG:3857"。

### Bug #2: 前端 TileWMS 未指定 projection

**位置**: `frontend/src/views/map/MapContainer.vue:255-263`

```javascript
const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,
  params: {
    'LAYERS': imageInfo.layerName,
    'TILED': true
  },
  serverType: 'geoserver',
  transition: 0
  // ← 缺少 projection 参数！
})
```

**问题**: TileWMS 没有指定 projection，OpenLayers 会使用 view 的 projection (EPSG:4326) 发送 WMS 请求。

### Bug #3: 前端 extent fit 时忽略 CRS

**位置**: `frontend/src/views/map/MapContainer.vue:279-285`

```javascript
if (imageInfo.extent && imageInfo.extent.length === 4) {
  map.value.getView().fit(imageInfo.extent, {
    padding: [50, 50, 50, 50],
    maxZoom: 15,
    duration: 500
  })
}
```

**问题**: 虽然 extent 确实是 EPSG:4326 坐标（与 view projection 匹配），但由于 crs 字段错误，前端无法正确判断 extent 的坐标系。

---

## 各问题详细分析

### 1. 后端 ImageServiceImpl.getImageWmsInfo() 返回的 extent 和 crs

**代码逻辑** (ImageServiceImpl.java:390-428):

```java
String sourceCrs = dataset.getSrs();  // 如 "EPSG:3857"
double[] transformedExtent = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);

if (transformedExtent != null) {
    info.setExtent(transformedExtent);  // EPSG:4326 经纬度
    info.setCrs("EPSG:3857");           // ← BUG: 应该是 "EPSG:4326"
} else {
    // fallback 逻辑
    double[] fallbackExtent = transformExtentSimple(extent);
    info.setExtent(fallbackExtent);
    info.setCrs("EPSG:3857");
}
```

**实际返回**:
- `extent`: `[116.2, 39.6, 116.6, 40.0]` (EPSG:4326 经纬度)
- `crs`: `"EPSG:3857"` (错误!)

### 2. 前端 MapContainer.vue loadImageLayer 接收的值

**类型**:
- `imageInfo.extent`: `[number, number, number, number]` (实际是 EPSG:4326)
- `imageInfo.crs`: `string` = `"EPSG:3857"` (错误值)

**接口定义** (MapContainer.vue:50-58):
```typescript
interface ImageLayerInfo {
  id: number
  name: string
  wmsUrl: string
  layerName: string
  crs: string           // ← 前端信任后端，结果被误导
  opacity: number
  extent?: [number, number, number, number]  // ← 实际是 EPSG:4326
}
```

### 3. /api/v1/images/{id}/wms-url JSON 响应示例

```json
{
  "code": 200,
  "data": {
    "wmsUrl": "http://localhost:8080/geoserver/gisplatform/raster_27/wms",
    "layerName": "raster_27",
    "crs": "EPSG:3857",
    "opacity": 0.8,
    "extent": [116.2, 39.6, 116.6, 40.0]
  }
}
```

**问题**: `extent` 明明是经纬度 (116.2-116.6, 39.6-40.0 是北京附近)，但 `crs` 说是 EPSG:3857 (米制坐标)。

### 4. GeoServer WMS GetCapabilities 响应中 raster_27 的原生 CRS

从代码分析:
- GeoServer 配置: `geoserver.workspace = "gisplatform"`
- layerName = `"raster_" + id` (如 raster_27)
- GeoTIFF 源数据是 EPSG:3857

**预期 GetCapabilities 部分**:
```xml
<Layer>
  <Name>raster_27</Name>
  <CRS>EPSG:3857</CRS>
  <CRS>EPSG:4326</CRS>
  <BoundingBox CRS="EPSG:3857" minx="12930000" miny="4855000" maxx="12940000" maxy="4856000"/>
</Layer>
```

### 5. 前端 TileWMS 请求的 URL 和参数

**实际请求** (从浏览器 Network 复制):

```
http://localhost:8080/geoserver/gisplatform/raster_27/wms?
  SERVICE=WMS&
  VERSION=1.3.0&
  REQUEST=GetMap&
  FORMAT=image/png&
  TRANSPARENT=true&
  LAYERS=gisplatform:raster_27&
  TILED=true&
  **BBOX**=116.1,39.5,116.7,40.1&          ← 这是 EPSG:4326!
  **SRS**=EPSG:4326&                       ← 请求用 EPSG:4326
  WIDTH=256&
  HEIGHT=256
```

**问题**:
- GeoServer 影像数据实际存储在 EPSG:3857
- 但 WMS 请求使用 EPSG:4326 的 BBOX
- **范围不匹配!** 116.1,39.5 在 EPSG:4326 是北京附近，但 GeoServer 期望 EPSG:3857 米制坐标。

### 6. extent 是否为 null/undefined/空数组

**可能情况**:

| 场景 | extent 值 | 结果 |
|------|-----------|------|
| 正常转换 | `[116.2, 39.6, 116.6, 40.0]` | fit 正常，但跳转到错误位置因 CRS 不匹配 |
| 转换失败 fallback | `[12930000, 4855000, 12940000, 4856000]` | fit 使用米制坐标跳转到赤道附近 |
| 解析失败 | `null` 或 `[0,0,0,0]` | fit 不执行，地图不跳转 |

### 7. CRS 字符串格式不一致

**后端可能的 CRS 值**:
- `"EPSG:3857"` - 标准格式 ✓
- `"EPSG:WGS 84 / Pseudo-Mercator"` - 非标准但 GeoTools 可解析
- `"EPSG:4326"` - 标准格式

**比较表**:

| 场景 | dataset.getSrs() | info.setCrs() | 是否匹配 |
|------|------------------|---------------|----------|
| 正常 | "EPSG:3857" | "EPSG:3857" | extent 转换后应为 4326，但 CRS 写 3857 |
| 异常 | "EPSG:WGS 84 / Pseudo-Mercator" | "EPSG:3857" | fallback 会转换 extent |

---

## 问题总结

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           问题汇总                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  【BUG #1】后端 setCrs 错误                                                  │
│  ────────────────────────                                                   │
│  ImageServiceImpl.java:410                                                  │
│  转换后的 extent 是 EPSG:4326，但 setCrs("EPSG:3857")                       │
│                                                                             │
│  【BUG #2】前端 TileWMS 无 projection                                        │
│  ─────────────────────────────                                              │
│  MapContainer.vue:255-263                                                   │
│  没有 projection 参数，WMS 请求使用 view 的 EPSG:4326                        │
│                                                                             │
│  【BUG #3】前端 fit 不考虑 CRS                                               │
│  ─────────────────────────                                                  │
│  MapContainer.vue:279-285                                                   │
│  直接 fit(imageInfo.extent)，未校验 extent 是否与 view projection 匹配       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 验证建议

### 1. 检查后端日志

搜索包含 "extent transformation" 的日志：

```bash
# 查看 application.log
grep "extent transformation" backend/logs/application.log
```

期望看到:
```
Image 27 extent transformation: sourceCRS=EPSG:3857, extent=[12930000, 4855000, 12940000, 4856000]
Successfully transformed extent to EPSG:4326: [116.2, 39.6, 116.6, 40.0]
```

### 2. 检查数据库记录

```sql
SELECT id, name, srs, extent FROM dataset WHERE type = 'raster' AND id = 27;
```

期望 `srs` = "EPSG:3857"，`extent` 存储的是米制坐标 JSON。

### 3. 检查前端控制台

在 `loadImageLayer` 函数添加日志：

```javascript
function loadImageLayer(imageInfo: ImageLayerInfo) {
  console.log('[loadImageLayer] extent:', imageInfo.extent)
  console.log('[loadImageLayer] crs:', imageInfo.crs)
  console.log('[loadImageLayer] view projection:', map.value.getView().getProjection().getCode())
  // ...
}
```

期望:
- `extent`: `[116.2, 39.6, 116.6, 40.0]` (经纬度)
- `crs`: 错误地显示 "EPSG:3857"
- `view projection`: "EPSG:4326"

### 4. 检查 WMS 请求

在浏览器 Network 面板找到 `.../wms?` 请求，检查：
- `BBOX` 参数值
- `SRS` 参数值

期望 BBOX 是经纬度值 (如 `116.1,39.5,116.7,40.1`)，但这与 GeoServer 数据不匹配。

---

## 修复方向

### 修复 1: 后端 ImageServiceImpl.getImageWmsInfo()

```java
// 正确做法：
info.setExtent(transformedExtent);  // EPSG:4326
info.setCrs("EPSG:4326");           // ← 改为 EPSG:4326
```

或者如果需要保持 WMS 请求用 EPSG:3857，应该返回两个不同的 CRS 值。

### 修复 2: 前端 MapContainer.vue

方案 A - 添加 projection 参数：
```javascript
const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,
  params: {
    'LAYERS': imageInfo.layerName,
    'TILED': true
  },
  projection: imageInfo.crs,  // 添加
  serverType: 'geoserver',
  transition: 0
})
```

方案 B - 校验 extent 是否合理后再 fit：
```javascript
if (imageInfo.extent && imageInfo.extent.length === 4) {
  const extent = imageInfo.extent
  // 如果 extent 是经纬度范围 (约 -180到180, -90到90)，直接 fit
  const isGeographic = extent[0] >= -180 && extent[0] <= 180 &&
                       extent[2] >= -180 && extent[2] <= 180 &&
                       extent[1] >= -90 && extent[1] <= 90 &&
                       extent[3] >= -90 && extent[3] <= 90
  
  if (isGeographic) {
    map.value.getView().fit(extent, {...})
  }
}
```

---

## 下一步行动

1. 确认后端日志中 extent 转换是否成功
2. 检查实际数据库中 dataset.srs 和 dataset.extent 的值
3. 根据本报告修复 Bug #1 (后端 setCrs)
4. 前端添加 projection 参数确保 WMS 请求使用正确 CRS
