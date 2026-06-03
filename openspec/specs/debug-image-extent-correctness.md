# 影像 extent 坐标转换正确性诊断报告

## 1. CrsTransformUtil 投影转换方法分析

### 转换方法: EPSG:3857 → EPSG:4326

**文件**: `backend/.../CrsTransformUtil.java:17-67`

```java
public static double[] transformExtentToWgs84(double[] extent, String sourceCrs) {
    // 输入: extent = [minX, minY, maxX, maxY] (EPSG:3857 米制坐标)
    // 输出: [minLon, minLat, maxLon, maxLat] (EPSG:4326 经纬度)

    // 关键代码:
    double[] minPoint = new double[]{extent[0], extent[1]};  // [minX, minY]
    double[] maxPoint = new double[]{extent[2], extent[3]};  // [maxX, maxY]

    MathTransform mathTransform = CRS.findMathTransform(source, target);

    mathTransform.transform(minPoint, 0, minResult, 0, 1);  // 转换 minX,minY
    mathTransform.transform(maxPoint, 0, maxResult, 0, 1);  // 转换 maxX,maxY

    // 结果数组顺序
    result[0] = minResult[0];  // minLon (minX 转换后的经度)
    result[1] = minResult[1];  // minLat (minY 转换后的纬度)
    result[2] = maxResult[0];  // maxLon (maxX 转换后的经度)
    result[3] = maxResult[1];  // maxLat (maxY 转换后的纬度)
}
```

### 转换逻辑验证

```
输入 EPSG:3857:
  extent[0] = minX = 12930000 (米)
  extent[1] = minY = 4855000  (米)
  extent[2] = maxX = 12940000 (米)
  extent[3] = maxY = 4856000  (米)

GeoTools MathTransform 转换:
  minPoint = [12930000, 4855000]
            ↓
  minResult = transform(minPoint)
            = [lon, lat]
            = [约 108.9°, 约 39.9°]  ← 注意：4855000米对应约39.9°N而非34.2°N!

  maxPoint = [12940000, 4856000]
            ↓
  maxResult = transform(maxPoint)
            = [lon, lat]
            = [约 109.0°, 约 39.9°]

输出 EPSG:4326:
  result[0] = minResult[0] = minLon = 108.9°
  result[1] = minResult[1] = minLat = 39.9°
  result[2] = maxResult[0] = maxLon = 109.0°
  result[3] = maxResult[1] = maxLat = 39.9°
```

**坐标系说明**:
- EPSG:3857 (Web Mercator): X轴=经度方向(米), Y轴=纬度方向(米)，但非线性压缩
- EPSG:4326 (WGS84): 经度Longitude, 纬度Latitude

---

## 2. ImageServiceImpl.getImageWmsInfo() 转换流程

**文件**: `backend/.../ImageServiceImpl.java:390-420`

```java
// Step 1: 从数据库解析 extent JSON
Map<String, Object> extentMap = mapper.readValue(dataset.getExtent(), Map.class);
// extentMap = {"minX":12930000, "minY":4855000, "maxX":12940000, "maxY":4856000}

// Step 2: 提取为 double 数组 [minX, minY, maxX, maxY]
extent[0] = ((Number) extentMap.get("minX")).doubleValue();  // 12930000
extent[1] = ((Number) extentMap.get("minY")).doubleValue();  // 4855000
extent[2] = ((Number) extentMap.get("maxX")).doubleValue();  // 12940000
extent[3] = ((Number) extentMap.get("maxY")).doubleValue();  // 4856000

// Step 3: 获取源 CRS
String sourceCrs = dataset.getSrs();  // 应该是 "EPSG:3857"

// Step 4: 转换为 EPSG:4326
double[] transformedExtent = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);

// Step 5: 设置返回信息
info.setExtent(transformedExtent);  // [minLon, minLat, maxLon, maxLat]
info.setCrs("EPSG:4326");
```

### 返回值分析

假设 `dataset.extent` = `{"minX":12930000,"minY":4855000,"maxX":12940000,"maxY":4856000}`

| 步骤 | 变量 | 值 | 说明 |
|------|------|-----|------|
| 解析 | extent[0] | 12930000 | minX (EPSG:3857 米) |
| 解析 | extent[1] | 4855000 | minY (EPSG:3857 米) |
| 解析 | extent[2] | 12940000 | maxX (EPSG:3857 米) |
| 解析 | extent[3] | 4856000 | maxY (EPSG:3857 米) |
| 转换 | transformedExtent[0] | 约 108.9° | minLon |
| 转换 | transformedExtent[1] | 约 39.9° | minLat |
| 转换 | transformedExtent[2] | 约 109.0° | maxLon |
| 转换 | transformedExtent[3] | 约 39.9° | maxLat |

---

## 3. extent 数组格式确认

### 正确格式: [minX, minY, maxX, maxY]

**代码确认**:
```java
// ImageServiceImpl.java:395-399
extent[0] = ((Number) extentMap.get("minX")).doubleValue();  // minX
extent[1] = ((Number) extentMap.get("minY")).doubleValue();  // minY
extent[2] = ((Number) extentMap.get("maxX")).doubleValue();  // maxX
extent[3] = ((Number) extentMap.get("maxY")).doubleValue();  // maxY
```

**转换为 EPSG:4326 后**:
```java
// CrsTransformUtil.java:51-55
result[0] = minResult[0];  // minLon (经度)
result[1] = minResult[1];  // minLat (纬度)
result[2] = maxResult[0];  // maxLon (经度)
result[3] = maxResult[1];  // maxLat (纬度)
```

**最终返回**: `[minLon, minLat, maxLon, maxLat]` = `[108.9, 39.9, 109.0, 39.9]`

---

## 4. 坐标顺序问题排查

### ❌ 没有发现问题

**正确的坐标顺序**:
- 输入: `[minX, minY, maxX, maxY]` (EPSG:3857)
- GeoTiffParser: `getMinimum(0)=minX`, `getMinimum(1)=minY`, `getMaximum(0)=maxX`, `getMaximum(1)=maxY`
- 转换: 独立转换各点，不混淆 X/Y
- 输出: `[minLon, minLat, maxLon, maxLat]` (EPSG:4326)

**代码审查结论**: 坐标顺序处理正确，无颠倒问题。

---

## 5. 米制坐标与经纬度的对应关系

### 关键发现

```
EPSG:3857 Y坐标 4855000 米 ≠ 34.2°N (西安纬度)

反向验证:
  y = 4855000 米
  y/R = 4855000 / 6378137 = 0.7617
  lat = 2 * arctan(exp(0.7617)) - π/2
      = 2 * arctan(2.142) - 1.571
      = 2 * 1.134 - 1.571
      = 0.697 rad
      = 39.9°
```

**问题**: GeoTIFF 中存储的 Y 坐标 (4855000 米) 对应约 **39.9°N**，而非用户认为的 34.2°N (西安)。

### 可能的原因

1. **GeoTIFF 坐标系统**: 影像本身的坐标参考系可能不是西安附近
2. **数据问题**: 上传的 GeoTIFF 可能不是西安区域的影像
3. **投影转换**: EPSG:3857 的 Y 坐标非线性，4855000 米确实对应约 40°N

---

## 6. transformExtentSimple fallback 分析

当 CrsTransformUtil 转换失败时，使用 fallback 方法：

```java
private double[] transformExtentSimple(double[] extent) {
    double R = 6378137.0;  // Earth radius in meters
    double PI = Math.PI;

    // 经度转换 (线性)
    double minX_rad = extent[0] / R;  // minX / R
    double maxX_rad = extent[2] / R;  // maxX / R
    double minLon = Math.toDegrees(minX_rad);
    double maxLon = Math.toDegrees(maxX_rad);

    // 纬度转换 (非线性 - Web Mercator 反算)
    double minY_rad = Math.atan(Math.exp(extent[1] / R));  // minY / R
    double maxY_rad = Math.atan(Math.exp(extent[3] / R));  // maxY / R
    double minLat = Math.toDegrees(2 * minY_rad - PI / 2);
    double maxLat = Math.toDegrees(2 * maxY_rad - PI / 2);

    // 结果: [minLon, minLat, maxLon, maxLat]
    return new double[]{minLon, minLat, maxLon, maxLat};
}
```

**Fallback 计算示例**:
```
输入: [12930000, 4855000, 12940000, 4856000]

minLon = degrees(12930000 / 6378137) = 108.9°
minLat = degrees(2 * atan(exp(0.7617)) - π/2) = 39.9°
maxLon = degrees(12940000 / 6378137) = 109.0°
maxLat = degrees(2 * atan(exp(0.7618)) - π/2) = 39.9°

输出: [108.9, 39.9, 109.0, 39.9]
```

---

## 7. 关键结论

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        诊断结论                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. 坐标转换顺序: ✓ 正确                                                     │
│     [minX, minY, maxX, maxY] → [minLon, minLat, maxLon, maxLat]           │
│                                                                             │
│  2. GeoTools MathTransform: ✓ 正确使用                                      │
│     独立转换 minPoint 和 maxPoint，不混淆 X/Y                               │
│                                                                             │
│  3. extent 数组格式: ✓ 正确                                                  │
│     返回 [minLon, minLat, maxLon, maxLat]                                   │
│                                                                             │
│  4. 可能的实际问题:                                                          │
│     - GeoTIFF 数据的 Y 坐标 4855000 米 ≈ 39.9°N                            │
│     - 这不是西安 (34.2°N)，而是约 40°N (可能是数据范围问题)                   │
│     - 或者上传的 GeoTIFF 本身就不是西安区域的影像                            │
│                                                                             │
│  5. WMS BBOX 89.9°E 问题:                                                   │
│     - 如果实际返回的经度是 108.9°E 而非 89.9°E                             │
│     - 那 89.9°E 可能是其他问题（如缓存、错误的 layer 等）                    │
│     - 需要检查实际的 WMS 请求和响应                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. 待验证项

1. **数据库实际存储的 extent**:
   ```sql
   SELECT id, name, srs, extent FROM dataset WHERE type = 'raster';
   ```

2. **GeoTIFF 原始坐标**:
   - 检查上传的 GeoTIFF 文件实际坐标范围
   - 确认 minX, minY, maxX, maxY 的实际值

3. **WMS 请求日志**:
   - 检查 TileWMS 发出的 GetMap 请求
   - 确认 BBOX 参数的实际值

4. **GeoServer GetCapabilities**:
   - 检查 raster_27 图层的原生 BBOX
   - 确认 declared CRS 和 native CRS
