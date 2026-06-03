# 影像 extent 经纬度顺序问题 - 精确定位诊断

## 用户描述的问题

API 返回: `[minLat, minLon, maxLat, maxLon]` = `[34.16, 108.96, 34.17, 108.97]`
期望: `[minLon, minLat, maxLon, maxLat]` = `[108.96, 34.16, 108.97, 34.17]`

---

## 代码追踪

### 1. GeoTiffParser 构建 extent JSON

**文件**: `backend/.../GeoTiffParser.java:96-115`

```java
Object envelope = coverage.getEnvelope();
Map<String, Object> transformMap = new HashMap<>();
try {
    Method getMin0 = envelope.getClass().getMethod("getMinimum", int.class);
    Method getMax0 = envelope.getClass().getMethod("getMaximum", int.class);
    transformMap.put("minX", getMin0.invoke(envelope, 0));  // dimension 0 minimum
    transformMap.put("minY", getMin0.invoke(envelope, 1));  // dimension 1 minimum
    transformMap.put("maxX", getMax0.invoke(envelope, 0));  // dimension 0 maximum
    transformMap.put("maxY", getMax0.invoke(envelope, 1));  // dimension 1 maximum
} catch (Exception e) { ... }
metadata.setTransform(MAPPER.writeValueAsString(transformMap));
```

**关键问题**: GeoTools Envelope 的 dimension 0 和 1 取决于 CRS 的 axis order。

| CRS 类型 | dimension 0 | dimension 1 |
|----------|-------------|-------------|
| EPSG:4326 (Lon, Lat) | 经度 (Lon) | 纬度 (Lat) |
| EPSG:4326 (Lat, Lon) | 纬度 (Lat) | 经度 (Lon) |
| EPSG:3857 (X, Y) | X (Easting) | Y (Northing) |

**可能问题**: 如果 GeoTIFF 的 CRS 声明为 (Lat, Lon) 顺序，但代码假设 (Lon, Lat) 顺序，则 `minX` 和 `minY` 会被颠倒赋值。

---

### 2. ImageServiceImpl 解析 extent JSON

**文件**: `backend/.../ImageServiceImpl.java:393-406`

```java
Map<String, Object> extentMap = mapper.readValue(dataset.getExtent(), Map.class);
double[] extent = new double[4];
extent[0] = ((Number) extentMap.get("minX")).doubleValue();  // 可能是 Lat!
extent[1] = ((Number) extentMap.get("minY")).doubleValue();  // 可能是 Lon!
extent[2] = ((Number) extentMap.get("maxX")).doubleValue();
extent[3] = ((Number) extentMap.get("maxY")).doubleValue();

double[] transformedExtent = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);
```

**问题**: extent 数组被填充为 [minX, minY, maxX, maxY]，但 minX/minY 可能已经是 lat/lon 而非 lon/lat。

---

### 3. CrsTransformUtil 转换

**文件**: `backend/.../CrsTransformUtil.java:42-55`

```java
double[] minPoint = new double[]{extent[0], extent[1]};  // [minX, minY]
double[] maxPoint = new double[]{extent[2], extent[3]};  // [maxX, maxY]

mathTransform.transform(minPoint, 0, minResult, 0, 1);
mathTransform.transform(maxPoint, 0, maxResult, 0, 1);

double[] result = new double[4];
result[0] = minResult[0];  // minLon
result[1] = minResult[1];  // minLat
result[2] = maxResult[0];  // maxLon
result[3] = maxResult[1];  // maxLat
```

**假设**: extent 输入是 [minX, minY] = [lon, lat] = [108.96, 34.16]

**转换结果**: minPoint = [108.96, 34.16] → minResult = [108.96, 34.16]
**输出**: [minLon, minLat, maxLon, maxLat] = [108.96, 34.16, ...]

---

## 根因分析

### 场景重建

如果 GeoTIFF 的 CRS axis order 是 (Lat, Lon)，则：

| JSON 字段 | 实际含义 | 值 |
|-----------|----------|-----|
| minX | minLat | 34.16° |
| minY | minLon | 108.96° |
| maxX | maxLat | 34.17° |
| maxY | maxLon | 108.97° |

**代码执行**:
```java
extent[0] = minX = 34.16   // 被当作 lon，但实际是 lat!
extent[1] = minY = 108.96  // 被当作 lat，但实际是 lon!
```

**转换时**: minPoint = [34.16, 108.96] 作为 EPSG:4326 lon/lat 处理
- 34.16 被当作 lon (范围应在 -180~180) ✓
- 108.96 被当作 lat (范围应在 -90~90) ✓

**GeoTools MathTransform**: 假设输入是 lon/lat，会尝试转换。但如果源 CRS 是 EPSG:3857 且坐标轴顺序是 (Lat, Lon)，则 MathTransform 会得到错误结果。

---

## 问题定位

### 可能的 Bug 位置

**位置 1**: GeoTiffParser.java:102-105

```java
// 当前代码
transformMap.put("minX", getMin0.invoke(envelope, 0));
transformMap.put("minY", getMin0.invoke(envelope, 1));  // 用错了 method!

// 应该用 getMin1 获取 dimension 1
```

等等，让我再检查...

```java
Method getMin0 = envelope.getClass().getMethod("getMinimum", int.class);
// ...
transformMap.put("minX", getMin0.invoke(envelope, 0));  // dimension 0 ✓
transformMap.put("minY", getMin0.invoke(envelope, 1));  // dimension 1 ✓
```

这看起来是正确的 - `getMin0` 是通用方法，通过参数指定 dimension。

### 真正的问题

真正的问题可能是 **GeoTIFF CRS axis order** 与代码假设不一致：

- 代码假设 EPSG:3857 是 (Lon, Lat) = (X, Y)
- 但某些 GeoTIFF 文件可能用 (Lat, Lon) 顺序声明 CRS

---

## 修复方案

### 方案 A: 在 GeoTiffParser 中检测并处理 axis order

```java
// 检查 CRS 的 axis order
CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
AxisDirection[] axes = crs.getCoordinateSystem().getAxis(0).getDirection().getDirection3D();
// 如果 axes[0] 是 northward 或 southward，说明第一个 axis 是 latitude
```

### 方案 B: 在 ImageServiceImpl 中交换 minX/minY

如果检测到 extent 像是 lat/lon 顺序而非 lon/lat，交换它们：

```java
double[] extent = new double[4];
double minX = ((Number) extentMap.get("minX")).doubleValue();
double minY = ((Number) extentMap.get("minY")).doubleValue();
double maxX = ((Number) extentMap.get("maxX")).doubleValue();
double maxY = ((Number) extentMap.get("maxY")).doubleValue();

// 检测是否是 lat/lon 顺序 (lat 应在 -90~90, lon 应在 -180~180)
if (minX >= -90 && minX <= 90 && minY >= -180 && minY <= 180) {
    // 看起来是 lat/lon 顺序 - 交换
    extent[0] = minY;  // lon
    extent[1] = minX;  // lat
    extent[2] = maxY;  // lon
    extent[3] = maxX;  // lat
} else {
    // 正常 lon/lat 顺序
    extent[0] = minX;
    extent[1] = minY;
    extent[2] = maxX;
    extent[3] = maxY;
}
```

---

## 验证步骤

1. 检查后端日志中 extent 转换的输入输出：
   ```
   Image <id> extent transformation: sourceCRS=EPSG:3857, extent=[?, ?, ?, ?]
   ```

2. 如果 extent 输入值中第一个值在 -90~90 范围内，说明可能是 lat/lon 顺序。

3. 确认 GeoTIFF 原始文件的 CRS 定义：
   ```bash
   gdalinfo image.tif | grep -i "coordinate system"
   ```

---

## 总结

| 问题 | 可能位置 | 说明 |
|------|----------|------|
| CRS axis order 不一致 | GeoTiffParser 或 ImageServiceImpl | GeoTIFF 使用 (Lat, Lon)，代码假设 (Lon, Lat) |
| 解决方案 | ImageServiceImpl.getImageWmsInfo() | 在转换前检测并交换 minX/minY 如果需要 |
