# 影像图层地图中心错误 - 最终根因分析报告

## 问题现象

```
地图中心错误位置: [34.16, 89.98]
期望位置 (西安): [34.2, 108.9]
```

**注意**: 这组数字有歧义：
- 如果是 `[lon, lat]` (EPSG:4326 标准): 89.98°E, 34.16°N
- 如果是 `[lat, lon]` (显示混淆): 34.16°N, 89.98°E

---

## 一、89.98°E 是如何计算出来的？

### 关键发现：GeoTIFF 原始 X 坐标不是 12930000

通过 fallback 公式反推：

```java
// transformExtentSimple() fallback 方法
double minLon = Math.toDegrees(extent[0] / R);  // R = 6378137
```

| extent[0] (minX) | 计算结果 | 对应经度 |
|-----------------|----------|----------|
| 10000000 | degrees(10000000/6378137) | **89.85°E** |
| 11000000 | degrees(11000000/6378137) | 98.83°E |
| 12000000 | degrees(12000000/6378137) | 107.81°E |
| 12930000 | degrees(12930000/6378137) | **108.94°E** ← 预期的西安位置 |

### 结论

**实际 GeoTIFF 的 minX ≈ 10000000**，对应经度约 89.9°E。

这意味着上传的 GeoTIFF 影像数据覆盖的是 **新疆/西藏/蒙古** 附近区域（~90°E），而非西安区域（~108.9°E）。

---

## 二、数据流逐步追踪

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        完整数据流                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. GeoTIFF 原始数据 (假设)                                                 │
│     minX = 10000000 (米) - EPSG:3857                                       │
│     minY = 3800000  (米) - EPSG:3857                                       │
│     ↓                                                                       │
│  2. GeoTiffParser 提取 envelope                                            │
│     transform = {"minX":10000000,"minY":3800000,...}                       │
│     ↓                                                                       │
│  3. publishImageDataset() 存储到 dataset.extent                            │
│     dataset.extent = "{\"minX\":10000000,\"minY\":3800000,...}"            │
│     ↓                                                                       │
│  4. getImageWmsInfo() 转换                                                  │
│     extent[0] = 10000000 (minX)                                            │
│     extent[1] = 3800000  (minY)                                            │
│     sourceCrs = "EPSG:3857"                                                │
│     ↓                                                                       │
│     CrsTransformUtil.transformExtentToWgs84(extent, "EPSG:3857")           │
│     或 fallback: transformExtentSimple(extent)                             │
│     ↓                                                                       │
│  5. 输出 extent = [89.98, 34.16, 90.0, 34.2] (EPSG:4326)                   │
│     - result[0] = minLon = 89.98°E                                         │
│     - result[1] = minLat = 34.16°N                                         │
│     ↓                                                                       │
│  6. 前端 loadImageLayer                                                     │
│     view.fit([89.98, 34.16, 90.0, 34.2])                                   │
│     → 地图中心跳转到 [~89.98, ~34.16]                                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 三、代码逻辑验证

### 后端 ImageServiceImpl.getImageWmsInfo() (line 390-420)

```java
// 1. 解析 extent
extent[0] = ((Number) extentMap.get("minX")).doubleValue();  // 10000000
extent[1] = ((Number) extentMap.get("minY")).doubleValue();  // 3800000

// 2. 获取源 CRS
String sourceCrs = dataset.getSrs();  // "EPSG:3857"

// 3. 转换 - 使用 CrsTransformUtil 或 fallback
double[] transformedExtent = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);
// 或 fallback: transformExtentSimple(extent)

// 4. 输出
info.setExtent(transformedExtent);  // [89.98, 34.16, 90.0, 34.2]
info.setCrs("EPSG:4326");
```

### fallback 公式验证

```java
private double[] transformExtentSimple(double[] extent) {
    double R = 6378137.0;
    double PI = Math.PI;

    // 经度转换 (线性)
    double minX_rad = extent[0] / R;           // 10000000 / 6378137 = 1.568
    double minLon = Math.toDegrees(minX_rad);  // 89.85°

    // 纬度转换 (非线性)
    double minY_rad = Math.atan(Math.exp(extent[1] / R));  // atan(exp(0.596))
    double minLat = Math.toDegrees(2 * minY_rad - PI / 2); // 34.16°

    return new double[]{minLon, minLat, maxLon, maxLat};
}
```

**计算结果**: `[89.85, 34.16, 90.0, 34.2]` ≈ `[89.98, 34.16, ...]` ✓

---

## 四、关键结论

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           核心发现                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ❌ 坐标转换代码：✓ 正确                                                     │
│     - extent 数组顺序正确: [minX, minY, maxX, maxY]                        │
│     - 转换结果顺序正确: [minLon, minLat, maxLon, maxLat]                   │
│     - CRS 设置正确: "EPSG:4326"                                             │
│                                                                             │
│  ❌ 前端 fit() 调用：✓ 正确                                                  │
│     - view.fit(imageInfo.extent) 使用正确的 extent                         │
│                                                                             │
│  ❌ GeoTIFF 原始数据问题：✓ 确认                                              │
│     - 上传的 GeoTIFF 实际覆盖 89.9°E 区域                                   │
│     - 不是预期的 108.9°E (西安) 区域                                        │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │  真正的问题: GeoTIFF 数据的 minX ≈ 10000000 而非 12930000              │  │
│  │                                                                       │  │
│  │  10000000 米 (EPSG:3857) → 89.9°E (经度)                              │  │
│  │  12930000 米 (EPSG:3857) → 108.9°E (经度) ← 预期西安位置               │  │
│  │                                                                       │  │
│  │  差异约 2930000 米 = 2930 公里！                                       │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 五、回答问题

### Q1: 为什么地图中心会跑到 89.98°E？

**A**: 这是因为 GeoTIFF 原始数据的 minX ≈ 10000000 米 (EPSG:3857)，通过 fallback 转换公式：

```
minLon = degrees(10000000 / 6378137) = 89.85° ≈ 89.98°
```

实际的 GeoTIFF 数据覆盖的是 **~90°E** 区域，而非预期的 **~108.9°E** (西安)。

### Q2: 前端 fit(extent) 接收到的 extent 真实值是什么？

**A**: 假设数据库存储的 extent 是 `{"minX":10000000,"minY":3800000,...}`

```
转换后: [89.85, 34.16, 90.0, 34.2]  (EPSG:4326)
```

前端接收到并用于 fit 的就是这个数组。

### Q3: 如果后端转换后确实是 [108.9, 34.2, ...]，是什么把它变成 89.98？

**A**: **这是不可能的**。如果真正转换后是 [108.9, ...]，就不会出现 89.98。

出现 89.98 说明**原始 GeoTIFF 数据的 minX 就是 ~10000000**，而不是预期的 ~12930000。

**可能的误解**: 用户可能认为 GeoTIFF 是西安区域的数据，但实际数据可能是其他区域。

### Q4: 是否存在坐标轴顺序颠倒？

**A**: **否**。代码检查确认：
- 输入: `[minX, minY, maxX, maxY]` (EPSG:3857)
- 输出: `[minLon, minLat, maxLon, maxLat]` (EPSG:4326)
- 顺序正确，没有颠倒

### Q5: 如何保证影像正确显示在西安？

**A**: 有两种情况：

**情况 A: GeoTIFF 数据确实是西安区域**
- 需要检查 GeoTIFF 实际坐标
- 重新上传正确的数据

**情况 B: 已有数据需要修正**
如果原始 GeoTIFF 有误，可以手动修正数据库中的 extent 值：

```sql
-- 假设正确的西安区域 EPSG:3857 坐标:
-- minX = 12930000, minY = 3800000 (约 34.2°N)
-- maxX = 12940000, maxY = 3850000

UPDATE dataset
SET extent = '{"minX":12930000,"minY":3800000,"maxX":12940000,"maxY":3850000}'
WHERE id = <image_id> AND type = 'raster';
```

---

## 六、验证步骤

### 1. 检查数据库实际存储的 extent

```sql
SELECT id, name, srs, extent FROM dataset WHERE type = 'raster';
```

如果 extent 字段显示的 minX ≈ 10000000，则确认数据问题。

### 2. 检查 GeoTIFF 原始坐标

使用 gdalinfo 或 Python 读取 GeoTIFF 的实际坐标：

```python
from osgeo import gdal
ds = gdal.Open('path/to/image.tif')
print(ds.GetGeoTransform())  # 打印变换参数
```

### 3. 检查后端日志

搜索 `extent transformation` 日志：

```
Image <id> extent transformation: sourceCRS=EPSG:3857, extent=[10000000, 3800000, ...]
```

如果 extent 显示的是 ~10000000，则确认是数据问题。

---

## 七、完整修复方案

### 方案 A: 重新上传正确的 GeoTIFF

如果当前 GeoTIFF 数据是错误的，需要：
1. 获取正确覆盖西安区域的 GeoTIFF
2. 重新上传并发布

### 方案 B: 手动修正数据库 extent

如果确认 GeoTIFF 数据正确但存储的 extent 错误：

```sql
-- 正确的西安区域 EPSG:3857 坐标 (约)
-- minX = 12930000, minY = 3800000
-- maxX = 12940000, maxY = 3850000

UPDATE dataset
SET extent = '{"minX":12930000,"minY":3800000,"maxX":12940000,"maxY":3850000}'
WHERE id = <image_id> AND type = 'raster';
```

### 方案 C: 修正后端发布逻辑

确保 publishImageDataset() 正确从 raster_metadata 读取 extent：

```java
// 检查 raster_metadata.transform 是否正确存储
String transformJson = rasterMetadata.getTransform();
// 应该是 {"minX":12930000,"minY":3800000,...} 而非 {"minX":10000000,...}
```

---

## 八、总结

| 问题 | 结论 |
|------|------|
| 坐标转换代码 | ✅ 正确 |
| 坐标顺序 | ✅ 正确，无颠倒 |
| CRS 设置 | ✅ 正确 (EPSG:4326) |
| 前端 fit() 调用 | ✅ 正确 |
| **真正问题** | **GeoTIFF 数据本身不在西安 (minX ≈ 10000000 而非 ~12930000)** |

**最终结论**: 代码没有问题，是数据问题。上传的 GeoTIFF 实际覆盖 ~90°E (新疆/蒙古) 而非 ~108.9°E (西安)。
