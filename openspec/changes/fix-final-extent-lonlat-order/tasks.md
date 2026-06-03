# Tasks: fix-final-extent-lonlat-order

## Task 1: 修改 ImageServiceImpl.getImageWmsInfo() extent 解析顺序 ✓

**文件**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

**位置**: 第 395-399 行

**修改内容**:

```java
// 修改前
double[] extent = new double[4];
extent[0] = ((Number) extentMap.get("minX")).doubleValue();
extent[1] = ((Number) extentMap.get("minY")).doubleValue();
extent[2] = ((Number) extentMap.get("maxX")).doubleValue();
extent[3] = ((Number) extentMap.get("maxY")).doubleValue();

// 修改后
double[] extent = new double[4];
extent[0] = ((Number) extentMap.get("minY")).doubleValue();  // minLon
extent[1] = ((Number) extentMap.get("minX")).doubleValue();  // minLat
extent[2] = ((Number) extentMap.get("maxY")).doubleValue();  // maxLon
extent[3] = ((Number) extentMap.get("maxX")).doubleValue();  // maxLat
```

**验证**: Maven 编译通过 ✓

---

## Task 2: 手动测试验证

1. 重启后端服务
2. 调用 `/api/v1/images/{id}/wms-url` API
3. 验证返回的 extent 数组顺序正确
4. 在地图上添加影像图层

**验证清单**:

- [ ] API 返回 extent[0] ≈ 108.96 (经度)
- [ ] API 返回 extent[1] ≈ 34.16 (纬度)
- [ ] 地图正确缩放到西安附近 (108.9°E, 34.2°N)
- [ ] 影像正确显示在地图上
