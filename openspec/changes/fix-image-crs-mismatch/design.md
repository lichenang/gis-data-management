# Design: fix-image-crs-mismatch

## 修复方案

### 1. 后端修复 - ImageServiceImpl.getImageWmsInfo()

**文件**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

**位置**: 第 408-410 行和第 416-417 行

**修改前**:
```java
if (transformedExtent != null) {
    info.setExtent(transformedExtent);
    info.setCrs("EPSG:3857");  // ← 错误：extent 已转换为 4326
} else {
    double[] fallbackExtent = transformExtentSimple(extent);
    info.setExtent(fallbackExtent);
    info.setCrs("EPSG:3857");  // ← 错误：fallback 转换后也是 4326
}
```

**修改后**:
```java
if (transformedExtent != null) {
    info.setExtent(transformedExtent);
    info.setCrs("EPSG:4326");  // ← 正确：extent 已转换为 4326
} else {
    double[] fallbackExtent = transformExtentSimple(extent);
    info.setExtent(fallbackExtent);
    info.setCrs("EPSG:4326");  // ← 正确：fallback 转换后也是 4326
}
```

**修改点**:
- Line 410: `"EPSG:3857"` → `"EPSG:4326"`
- Line 417: `"EPSG:3857"` → `"EPSG:4326"`

### 2. 前端修复 - MapContainer.vue TileWMS

**文件**: `frontend/src/views/map/MapContainer.vue`

**位置**: 第 255-263 行

**修改前**:
```typescript
const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,
  params: {
    'LAYERS': imageInfo.layerName,
    'TILED': true
  },
  serverType: 'geoserver',
  transition: 0
})
```

**修改后**:
```typescript
const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,
  params: {
    'LAYERS': imageInfo.layerName,
    'TILED': true
  },
  projection: imageInfo.crs || 'EPSG:4326',
  serverType: 'geoserver',
  transition: 0
})
```

**修改点**:
- 添加 `projection: imageInfo.crs || 'EPSG:4326'`

## 数据流（修复后）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     修复后的数据流                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  后端:                                                                      │
│  ────                                                                      │
│  1. dataset.srs = "EPSG:3857" (原始 CRS)                                    │
│  2. extent = [12930000, 4855000, 12940000, 4856000] (原始米制坐标)          │
│  3. CrsTransformUtil.transformExtentToWgs84() → 转换为 EPSG:4326           │
│  4. info.setExtent([116.2, 39.6, 116.6, 40.0])  ← EPSG:4326 经纬度         │
│  5. info.setCrs("EPSG:4326")                  ← 修复！与 extent 匹配        │
│                                                                             │
│  前端:                                                                      │
│  ────                                                                      │
│  1. imageInfo.extent = [116.2, 39.6, 116.6, 40.0]                          │
│  2. imageInfo.crs = "EPSG:4326"                                            │
│  3. view.fit(imageInfo.extent) → 正确缩放到北京附近                         │
│  4. TileWMS projection = "EPSG:4326"                                       │
│     → WMS 请求使用 EPSG:4326 BBOX                                          │
│     → GeoServer 返回正确影像（因为 layer 支持 EPSG:4326）                   │
│                                                                             │
│  结果:                                                                      │
│  ────                                                                      │
│  ✓ 地图正确缩放到影像区域                                                    │
│  ✓ 影像正确显示                                                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 完整代码修改

### Backend: ImageServiceImpl.java (lines 408-417)

```java
// 获取源 CRS 并转换 extent 到 EPSG:4326
String sourceCrs = dataset.getSrs();
double[] transformedExtent = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);

if (transformedExtent != null) {
    info.setExtent(transformedExtent);
    info.setCrs("EPSG:4326");  // ← 修复：与转换后的 extent 匹配
    log.info("Successfully transformed extent to EPSG:4326: [{}, {}, {}, {}]",
            transformedExtent[0], transformedExtent[1], transformedExtent[2], transformedExtent[3]);
} else {
    // GeoTools转换失败，使用备用数学换算
    double[] fallbackExtent = transformExtentSimple(extent);
    info.setExtent(fallbackExtent);
    info.setCrs("EPSG:4326");  // ← 修复：fallback 结果也是 EPSG:4326
    log.warn("Using fallback extent transformation for dataset {}. Fallsback: [{}, {}, {}, {}]",
            id, fallbackExtent[0], fallbackExtent[1], fallbackExtent[2], fallbackExtent[3]);
}
```

### Frontend: MapContainer.vue (lines 255-264)

```typescript
const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,
  params: {
    'LAYERS': imageInfo.layerName,
    'TILED': true
  },
  projection: imageInfo.crs || 'EPSG:4326',
  serverType: 'geoserver',
  transition: 0
})
```

## 验证步骤

1. Maven 编译后端
2. 启动前端开发服务器
3. 上传并发布 GeoTIFF 影像
4. 在地图上添加影像图层
5. 验证：
   - 地图自动缩放到正确区域（不是海里）
   - 影像正确显示
