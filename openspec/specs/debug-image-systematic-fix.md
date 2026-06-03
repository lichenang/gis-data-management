# 影像图层 BBOX 错误 - 系统性诊断报告

## 问题现象

- WMS 请求成功（200 OK）
- 但 BBOX 错误（89.9°E 而非 108.9°E，错误约 19°）
- 影像实际位置：西安飞天路（约 108.9°E, 34.2°N）

## 全链路数据流分析

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     数据流完整链路                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. GeoTIFF 上传                                                            │
│     ↓                                                                       │
│  2. GeoTiffParser.parse() 提取:                                             │
│     - crs: "EPSG:3857" (从 coverage.getCoordinateReferenceSystem())        │
│     - transform: {"minX": 12930000, "minY": 4855000, ...} (米制坐标)        │
│     ↓                                                                       │
│  3. uploadImage() 创建 Dataset:                                             │
│     - dataset.srs = "EPSG:3857"                                            │
│     - dataset.extent = null (此时还未设置)                                  │
│     ↓                                                                       │
│  4. publishImageDataset() 发布时:                                           │
│     - 从 raster_metadata.transform 解析 extent JSON                        │
│     - dataset.extent = "{\"minX\":12930000,\"minY\":4855000,...}"          │
│     ↓                                                                       │
│  5. getImageWmsInfo() 获取 WMS 信息:                                        │
│     - dataset.srs = "EPSG:3857"                                            │
│     - dataset.extent = "{\"minX\":12930000,...}"                           │
│     - CrsTransformUtil.transformExtentToWgs84(extent, "EPSG:3857")         │
│     - → 返回 [108.9, 34.1, 109.0, 34.2] (EPSG:4326 经纬度)                 │
│     - info.setCrs("EPSG:4326")                                             │
│     - info.setExtent([108.9, 34.1, 109.0, 34.2])                           │
│     ↓                                                                       │
│  6. 前端 MapContainer.vue                                                   │
│     - loadImageLayer(imageInfo)                                            │
│     - TileWMS: projection: 'EPSG:4326'                                     │
│     - view.fit([108.9, 34.1, 109.0, 34.2]) → 缩放到西安                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 1. 后端 ImageServiceImpl.getImageWmsInfo() 分析

**文件**: `backend/.../ImageServiceImpl.java:375-428`

### 当前代码逻辑

```java
// Line 390-428
if (dataset.getExtent() != null && !dataset.getExtent().isEmpty()) {
    // 1. 解析 extent JSON
    Map<String, Object> extentMap = mapper.readValue(dataset.getExtent(), Map.class);
    double[] extent = new double[4];  // [minX, minY, maxX, maxY]
    extent[0] = ((Number) extentMap.get("minX")).doubleValue();
    extent[1] = ((Number) extentMap.get("minY")).doubleValue();
    extent[2] = ((Number) extentMap.get("maxX")).doubleValue();
    extent[3] = ((Number) extentMap.get("maxY")).doubleValue();

    // 2. 获取源 CRS
    String sourceCrs = dataset.getSrs();  // 应该是 "EPSG:3857"

    // 3. 转换为 EPSG:4326
    double[] transformedExtent = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);

    // 4. 设置返回信息
    if (transformedExtent != null) {
        info.setExtent(transformedExtent);  // [108.9, 34.1, 109.0, 34.2]
        info.setCrs("EPSG:4326");           // ✓ 正确
    } else {
        info.setExtent(fallbackExtent);
        info.setCrs("EPSG:4326");
    }
}
```

### ✅ 确认：后端返回是正确的

- `info.setCrs("EPSG:4326")` - 正确
- `info.setExtent(transformedExtent)` - 转换后的 EPSG:4326 坐标

---

## 2. 前端 MapContainer.vue TileWMS 分析

**文件**: `frontend/src/views/map/MapContainer.vue:252-287`

### 当前代码

```typescript
function loadImageLayer(imageInfo: ImageLayerInfo) {
  const wmsSource = new TileWMS({
    url: imageInfo.wmsUrl,           // "/geoserver/gisplatform/raster_27/wms"
    params: {
      'LAYERS': imageInfo.layerName, // "raster_27"
      'TILED': true
    },
    projection: imageInfo.crs || 'EPSG:4326',  // "EPSG:4326" ✓
    serverType: 'geoserver',
    transition: 0
  })

  // fit 缩放到影像范围
  if (imageInfo.extent && imageInfo.extent.length === 4) {
    map.value.getView().fit(imageInfo.extent, {  // extent = [108.9, 34.1, 109.0, 34.2]
      padding: [50, 50, 50, 50],
      maxZoom: 15,
      duration: 500
    })
  }
}
```

### ✅ 确认：前端配置基本正确

- `projection: imageInfo.crs || 'EPSG:4326'` = "EPSG:4326" ✓
- `view.fit(imageInfo.extent)` 使用正确的 extent ✓

---

## 3. 可能的问题点分析

### 问题点 1: TileWMS 没有设置 extent 参数

**现象**: TileWMS 没有明确设置 `extent` 参数

**分析**:
```typescript
const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,
  params: { 'LAYERS': imageInfo.layerName, 'TILED': true },
  projection: 'EPSG:4326',
  serverType: 'geoserver',
  // 没有 extent 或 tileGrid 参数!
})
```

**影响**: OpenLayers 使用 view 的 extent 来计算需要请求的 tiles。如果 view.fit() 正确执行了，请求范围应该正确。

### 问题点 2: WMS 请求的 SRS/BBOX 参数

**期望的 GetMap 请求**:
```
/geoserver/gisplatform/raster_27/wms?
  SERVICE=WMS&
  VERSION=1.3.0&
  REQUEST=GetMap&
  FORMAT=image/png&
  LAYERS=gisplatform:raster_27&
  TILED=true&
  BBOX=108.8,34.0,109.1,34.3&        ← 西安附近 (EPSG:4326)
  SRS=EPSG:4326&                      ← 使用 EPSG:4326
  WIDTH=256&
  HEIGHT=256
```

**实际情况**: BBOX 可能是 89.9°E 而非 108.9°E

### 问题点 3: GeoServer 数据发布可能有问题

GeoServer 存储的是 EPSG:3857 的影像数据。当 TileWMS 请求 EPSG:4326 时：
1. OpenLayers 发送 EPSG:4326 BBOX
2. GeoServer 需要将 EPSG:4326 坐标转换为 EPSG:3857 来读取数据
3. 如果转换正确，返回正确影像

**可能问题**: GeoServer 发布时没有正确设置 CRS 或范围

---

## 4. 系统性检查清单

### 数据库检查

```sql
-- 检查 dataset 表
SELECT id, name, srs, extent FROM dataset WHERE type = 'raster' AND id = <image_id>;

-- 检查 raster_metadata 表
SELECT dataset_id, crs, transform FROM raster_metadata WHERE dataset_id = <image_id>;
```

**期望**:
- `srs`: "EPSG:3857"
- `extent`: 米制坐标 JSON 如 `{"minX":12930000,"minY":4855000,...}`

### 后端日志检查

搜索日志中 extent 转换信息：
```
Image <id> extent transformation: sourceCRS=EPSG:3857, extent=[12930000, 4855000, 12940000, 4856000]
Successfully transformed extent to EPSG:4326: [108.9, 34.1, 109.0, 34.2]
```

### 浏览器 Network 检查

在 Network 面板找到 WMS GetMap 请求，检查：
- `BBOX` 参数值是否是西安附近 (约 108.9°E, 34°N)
- `SRS` 或 `CRS` 参数是否是 `EPSG:4326`

---

## 5. 修复方案

### 方案 A: 添加 TileWMS extent 参数

确保 TileWMS 知道数据的有效范围：

```typescript
const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,
  params: {
    'LAYERS': imageInfo.layerName,
    'TILED': true
  },
  projection: 'EPSG:4326',
  extent: imageInfo.extent,  // ← 添加 extent
  serverType: 'geoserver',
  transition: 0
})
```

### 方案 B: 创建 ResolutionStrategy 或 tileGrid

```typescript
import TileGrid from 'ol/tilegrid/TileGrid'
import { get as getProjection } from 'ol/proj'

const projection = getProjection('EPSG:4326')
const tileGrid = new TileGrid({
  origin: [-180, -90],  // EPSG:4326 origin
  resolutions: [0.703125, 0.3515625, 0.17578125, ...],  // Zoom levels
  extent: imageInfo.extent  // Constrain to data extent
})

const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,
  params: { 'LAYERS': imageInfo.layerName, 'TILED': true },
  projection: projection,
  tileGrid: tileGrid,
  serverType: 'geoserver'
})
```

### 方案 C: 检查 GeoServer 发布设置

确认 GeoServer 图层的：
1. Native CRS = EPSG:3857
2. Declared SRS = EPSG:4326 (支持跨域请求)
3. Lat/Lon Bounding Box = 正确值

---

## 6. 验证步骤

1. **检查后端日志**
   - 确认 extent 转换成功
   - 确认 sourceCRS = "EPSG:3857"
   - 确认 transformedExtent = [108.9, 34.1, ...]

2. **检查数据库记录**
   - dataset.srs 应该是 "EPSG:3857"
   - dataset.extent 应该是米制坐标 JSON

3. **检查浏览器 WMS 请求**
   - BBOX 应该是 108.8,34.0,109.1,34.3 左右
   - SRS 应该是 EPSG:4326

4. **测试 GeoServer 直接请求**
   ```
   /geoserver/gisplatform/raster_27/wms?SERVICE=WMS&REQUEST=GetMap&BBOX=108.8,34.0,109.1,34.3&SRS=EPSG:4326&WIDTH=256&HEIGHT=256&FORMAT=image/png
   ```

---

## 7. 待用户提供的调试信息

1. 后端日志中 extent 转换的完整输出
2. 数据库查询结果：`SELECT id, srs, extent FROM dataset WHERE type = 'raster' LIMIT 5;`
3. 浏览器 Network 面板中 WMS GetMap 请求的完整 URL
