# Tasks: fix-extent-lonlat-order

## Task 1: 检查 CrsTransformUtil 代码确认 extent 顺序

**文件**: `backend/src/main/java/com/gisplatform/util/CrsTransformUtil.java`

**位置**: 第 51-55 行

**检查内容**:

确认当前代码的 extent 数组构建逻辑：

```java
double[] result = new double[4];
result[0] = minResult[0];  // 应该是 minLon
result[1] = minResult[1];  // 应该是 minLat
result[2] = maxResult[0];  // 应该是 maxLon
result[3] = maxResult[1];  // 应该是 maxLat
```

**期望结果**: 输出 `[minLon, minLat, maxLon, maxLat]`

---

## Task 2: 检查 transformExtentSimple fallback 顺序

**文件**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

**位置**: 第 439-445 行

**检查内容**:

```java
double[] result = new double[4];
result[0] = Math.toDegrees(minX_rad);              // minLon
result[1] = Math.toDegrees(2 * minY_rad - PI / 2); // minLat
result[2] = Math.toDegrees(maxX_rad);              // maxLon
result[3] = Math.toDegrees(2 * maxY_rad - PI / 2); // maxLat
```

**期望结果**: 输出 `[minLon, minLat, maxLon, maxLat]`

---

## Task 3: 手动测试验证

1. 启动后端服务
2. 调用 `getImageWmsInfo()` API
3. 检查返回的 extent 数组

**验证清单**:
- [ ] extent[0] < extent[2] (minLon < maxLon)
- [ ] extent[1] < extent[3] (minLat < maxLat)
- [ ] extent 值在合理范围内 (-180~180, -90~90)
