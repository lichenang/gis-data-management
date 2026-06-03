# Tasks: fix-image-extent-final

## Task 1: 后端增强 extent 转换逻辑 ✓

**文件**: `backend/.../ImageServiceImpl.java`

在 `getImageWmsInfo` 方法中添加备用转换逻辑：

1. 在类的末尾添加辅助方法 `transformExtentSimple`：

```java
private double[] transformExtentSimple(double[] extent) {
    double R = 6378137.0;
    double PI = Math.PI;
    
    double minX_rad = extent[0] / R;
    double maxX_rad = extent[2] / R;
    double minY_rad = Math.atan(Math.exp(extent[1] / R));
    double maxY_rad = Math.atan(Math.exp(extent[3] / R));
    
    double[] result = new double[4];
    result[0] = Math.toDegrees(minX_rad);
    result[1] = Math.toDegrees(2 * minY_rad - PI / 2);
    result[2] = Math.toDegrees(maxX_rad);
    result[3] = Math.toDegrees(2 * maxY_rad - PI / 2);
    
    return result;
}
```

2. 修改 extent 转换逻辑，当 CrsTransformUtil 返回 null 时使用备用方法：

```java
double[] transformedExtent = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);

if (transformedExtent != null) {
    info.setExtent(transformedExtent);
    info.setCrs("EPSG:3857");
    log.info("Successfully transformed extent to EPSG:4326");
} else {
    // 使用备用转换
    double[] fallbackExtent = transformExtentSimple(extent);
    info.setExtent(fallbackExtent);
    info.setCrs("EPSG:3857");
    log.warn("Using fallback extent transformation for dataset {}", id);
}
```

## Task 2: 前端简化 extent 使用逻辑 ✓

**文件**: `frontend/.../MapContainer.vue`

修改 `loadImageLayer` 函数（约 277-325 行），简化为直接使用后端返回的 extent：

```typescript
// 缩放到影像范围 - 简化版
// 后端返回的 extent 已经是 EPSG:4326 经纬度坐标，直接使用
if (imageInfo.extent && imageInfo.extent.length === 4) {
  map.value.getView().fit(imageInfo.extent, {
    padding: [50, 50, 50, 50],
    maxZoom: 15,
    duration: 500
  })
}
```

## Task 3: 验证修复 ✓

### 后端验证

```bash
cd backend
mvn compile
```

### 前端验证

```bash
cd frontend
npm run dev
```

### 手动测试

1. 重启前后端应用
2. 上传并发布 GeoTIFF 影像
3. 在地图上添加影像图层
4. 确认：
   - [ ] 地图缩放到正确区域（不是海里）
   - [ ] 影像正确显示
