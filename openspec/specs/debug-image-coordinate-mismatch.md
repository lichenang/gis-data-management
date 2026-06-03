# 影像坐标系不匹配问题诊断报告

## 问题现象

影像数据为 EPSG:3857，地图视图和 WMS 请求使用 EPSG:4326，导致地图加载后一片空白。

---

## 1. 前端 MapContainer.vue 中 OpenLayers View 的 projection 是什么？

**位置**: `frontend/src/views/map/MapContainer.vue:116` 和 `:169`

```javascript
view: new View({
  center: [116.4, 39.9],
  zoom: 10,
  projection: 'EPSG:4326'  // ← 视图使用 EPSG:4326
})
```

**结论**: View projection = **EPSG:4326** ✓

---

## 2. OSM 底图是什么坐标系？

**位置**: `frontend/src/views/map/MapContainer.vue:106-108`

```javascript
const baseLayer = new TileLayer({
  source: new OSM()
})
```

**分析**: OSM 瓦片使用 EPSG:3857 (Web Mercator)。OpenLayers 会自动将 OSM 瓦片重投影到视图的 EPSG:4326。

**结论**: OSM 底图 = **EPSG:3857** (OpenLayers 自动处理) ✓

---

## 3. 矢量图层（GeoJSON）的 featureProjection 是什么？

**位置**: `frontend/src/views/map/MapContainer.vue:197-199`

```javascript
const features = new GeoJSON().readFeatures(geojson, {
  featureProjection: 'EPSG:4326'
})
```

**同时**: VectorSource 也指定 projection
```javascript
const source: any = new VectorSource({
  projection: 'EPSG:4326',  // ← 源也指定为 4326
  // ...
})
```

**结论**: 矢量图层 featureProjection = **EPSG:4326** ✓

---

## 4. 影像 TileWMS source 的 projection 参数是什么？

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
  // ← 没有 projection 参数！
})
```

**问题分析**: 
- TileWMS 没有指定 `projection` 参数
- OpenLayers 默认使用 map 的 view projection (EPSG:4326) 发送 WMS 请求
- 但 GeoServer 的影像数据实际存储在 EPSG:3857

**Bug #1**: WMS 请求使用 EPSG:4326，但数据是 EPSG:3857

---

## 5. 是否在影像图层加载后调用了 fit(extent)，但 extent 是 EPSG:3857 值？

**位置**: `frontend/src/views/map/MapContainer.vue:278-285`

```javascript
// 缩放到影像范围
if (imageInfo.extent && imageInfo.extent.length === 4) {
  const extent: [number, number, number, number] = imageInfo.extent
  map.value.getView().fit(extent, {
    padding: [50, 50, 50, 50],
    maxZoom: 15,
    duration: 500
  })
}
```

**需要确认**: 前端收到的 extent 是什么坐标系？

**后端位置**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java:390-418`

```java
// 获取源 CRS 并转换 extent 到 EPSG:4326
String sourceCrs = dataset.getSrs();  // 这应该是 EPSG:3857
double[] transformedExtent = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);

if (transformedExtent != null) {
    info.setExtent(transformedExtent);   // ← 返回转换后的 EPSG:4326
    info.setCrs("EPSG:4326");
} else {
    info.setExtent(extent);              // ← 如果转换失败，使用原始值
}
```

**分析**: 
- 后端调用 `CrsTransformUtil.transformExtentToWgs84()` 尝试转换
- 应该返回 EPSG:4326 的 extent 给前端

**可能的 Bug #2**: 
- `dataset.getSrs()` 返回的是否是正确的 EPSG 代码？
- 之前修复的 `GeoTiffParser.extractCrs()` 是否生效？

---

## 6. 如果统一改为 EPSG:3857 视图，需要同步修改哪些地方？

```
┌─────────────────────────────────────────────────────────────────────────┐
│           统一 EPSG:3857 视图需要修改的位置                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  前端 (MapContainer.vue):                                              │
│  1. View projection: 'EPSG:4326' → 'EPSG:3857'                        │
│  2. VectorSource projection: 'EPSG:4326' → 'EPSG:3857'                │
│  3. GeoJSON featureProjection: 'EPSG:4326' → 'EPSG:3857'              │
│  4. 中心点坐标: [116.4, 39.9] → 米制坐标                               │
│     北京: 约 [12930000, 4855000] in EPSG:3857                         │
│                                                                         │
│  后端:                                                                  │
│  1. CrsTransformUtil.transformExtentToWgs84() → 无需调用              │
│  2. getImageWmsInfo() 返回原始 extent                                  │
│  3. WMS 请求 projection 自动匹配 (无改动)                              │
│                                                                         │
│  风险:                                                                  │
│  - OSM 底图本身是 EPSG:3857，无需重投影，性能更好                       │
│  - 但 EPSG:4326 是地理坐标，更适合覆盖全中国/全球                       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 7. 如果保持 EPSG:4326 视图，需要对 extent 做 CRS 转换，当前 CrsTransformUtil 是否生效？

**CrsTransformUtil 分析** (`backend/.../CrsTransformUtil.java`):

```java
public static double[] transformExtentToWgs84(double[] extent, String sourceCrs) {
    if ("EPSG:4326".equalsIgnoreCase(sourceCrs)) {
        return extent;  // 直接返回
    }

    try {
        CoordinateReferenceSystem source = CRS.decode(sourceCrs);
        CoordinateReferenceSystem target = CRS.decode(TARGET_CRS);
        MathTransform mathTransform = CRS.findMathTransform(source, target);
        // 转换逻辑...
    } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Failed to transform...");
        return null;
    }
}
```

**问题**: 如果 `sourceCrs` 是非标准格式（如 "EPSG:WGS 84 / Pseudo-Mercator"），`CRS.decode()` 会失败！

**验证步骤**:
1. 确认 `dataset.getSrs()` 返回的是 "EPSG:3857" 还是非标准格式
2. 如果仍是 "EPSG:WGS 84 / Pseudo-Mercator"，转换会失败返回 null

---

## 根因总结

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        问题链路                                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────────────────┐  │
│  │ GeoTIFF     │ ──▶ │ GeoTiff     │ ──▶ │ Dataset.srs             │  │
│  │ CRS: 3857   │     │ Parser      │     │ = ? (需验证修复是否生效) │  │
│  └─────────────┘     └─────────────┘     └───────────┬─────────────┘  │
│                                                       │                 │
│                                                       ▼                 │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │ ImageServiceImpl.getImageWmsInfo()                            │    │
│  │ 1. dataset.getExtent() → 原始 extent (EPSG:3857)             │    │
│  │ 2. dataset.getSrs() → "EPSG:3857" ?                          │    │
│  │ 3. CrsTransformUtil.transformExtentToWgs84()                 │    │
│  │    → 若 srs 格式错误，返回 null → 使用原始 extent！           │    │
│  └────────────────────────────┬─────────────────────────────────┘    │
│                               │                                        │
│                               ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │ 前端 MapContainer.vue                                          │  │
│  │ 1. 接收 extent (若转换失败，仍是 EPSG:3857)                    │  │
│  │ 2. view.fit(extent) → 使用 EPSG:3857 坐标 fit EPSG:4326 视图  │  │
│  │ 3. TileWMS 无 projection → WMS 请求用 EPSG:4326               │  │
│  │    → 但 GeoServer 数据是 3857 → 请求范围不匹配                │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 修复方案

### 方案 A: 修复后端 extent 转换（推荐）

确保后端正确转换 extent 到 EPSG:4326：

1. **验证 GeoTiffParser 修复是否生效** - 确认 `dataset.srs` = "EPSG:3857"
2. **确认 CrsTransformUtil 正常工作** - 添加日志确认转换成功

### 方案 B: 前端添加 projection 参数到 TileWMS

```javascript
const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,
  params: {
    'LAYERS': imageInfo.layerName,
    'TILED': true
  },
  projection: imageInfo.crs || 'EPSG:4326',  // ← 添加 projection
  serverType: 'geoserver',
  transition: 0
})
```

### 方案 C: 统一使用 EPSG:3857 视图（长期方案）

修改前端 View projection 为 EPSG:3857，一劳永逸。

---

## 待验证项

1. [ ] 检查数据库中 `dataset.srs` 字段实际存储的值
2. [ ] 日志确认 CrsTransformUtil 是否成功转换
3. [ ] 测试影像发布后，前端收到的 extent 值
