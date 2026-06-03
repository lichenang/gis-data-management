# Design: fix-image-crs-return-value

## 修复方案

### 修改 ImageServiceImpl.getImageWmsInfo()

**文件**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

修改 `getImageWmsInfo` 方法（约第 408-418 行），将 CRS 返回值改为原始 CRS：

```java
if (transformedExtent != null) {
    info.setExtent(transformedExtent);
    // 修改前（错误）: info.setCrs("EPSG:4326");
    // 修改后（正确）: 返回 GeoServer 影像的实际 CRS
    info.setCrs(sourceCrs != null ? sourceCrs : "EPSG:4326");
    log.info("Successfully transformed extent to EPSG:4326: [{}, {}, {}, {}]",
            transformedExtent[0], transformedExtent[1], transformedExtent[2], transformedExtent[3]);
} else {
    info.setExtent(extent);
    info.setCrs(sourceCrs != null ? sourceCrs : "EPSG:4326");
    log.warn("Failed to transform extent for dataset {}, using original extent. sourceCrs={}",
            id, sourceCrs);
}
```

## 数据流（修复后）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     修复后的数据流                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  后端:                                                                      │
│  dataset.srs = "EPSG:3857"                                                 │
│  dataset.extent = {minX, minY, maxX, maxY} (3857)                          │
│         │                                                                   │
│         ▼                                                                   │
│  getImageWmsInfo()                                                          │
│  ├─ CrsTransformUtil.transformExtentToWgs84()                             │
│  │   ✓ extent 转换为 EPSG:4326 (用于 fit 视图)                            │
│  └─ 返回:                                                                   │
│      {                                                                      │
│        extent: [lon1, lat1, lon2, lat2],  // 转换后的 EPSG:4326           │
│        crs: "EPSG:3857"  ← 正确！GeoServer 实际使用的 CRS                 │
│      }                                                                      │
│                                                                             │
│  前端:                                                                      │
│  imageInfo = {                                                              │
│    extent: [lon1, lat1, lon2, lat2],  // 已是 EPSG:4326，用于 fit        │
│    crs: "EPSG:3857"  ← 正确！告诉 WMS 请求用这个 CRS                      │
│  }                                                                          │
│         │                                                                   │
│         ▼                                                                   │
│  loadImageLayer()                                                           │
│  ├─ TileWMS(projection: "EPSG:3857")                                       │
│  │   ✓ WMS 请求使用正确的坐标系                                            │
│  └─ fit(extent)                                                            │
│      ✓ extent 已是 EPSG:4326，直接 fit                                    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 验证步骤

1. Maven 编译: `mvn compile -f backend/pom.xml`
2. 启动应用，调用 `GET /api/v1/images/{id}/wms-url`
3. 检查返回的 `crs` 字段是否为原始 CRS（如 "EPSG:3857"）
4. 在前端地图上添加影像图层
5. 确认 WMS 请求使用正确的 BBOX 格式
6. 确认影像正确显示
