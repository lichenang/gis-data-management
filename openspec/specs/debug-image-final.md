# 影像坐标系问题最终诊断报告

## 当前代码分析

### 1. 后端 ImageServiceImpl.getImageWmsInfo()

**文件**: `backend/.../ImageServiceImpl.java:390-419`

```java
String sourceCrs = dataset.getSrs();  // 从 dataset 表读取 srs 字段
log.info("Image {} extent transformation: sourceCRS={}, extent=[{}, {}, {}, {}]",
        id, sourceCrs, extent[0], extent[1], extent[2], extent[3]);

double[] transformedExtent = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);

if (transformedExtent != null) {
    info.setExtent(transformedExtent);
    info.setCrs(sourceCrs != null ? sourceCrs : "EPSG:4326");
    // ...
} else {
    info.setExtent(extent);  // 使用原始 extent（EPSG:3857）
    info.setCrs(sourceCrs != null ? sourceCrs : "EPSG:4326");
    // 警告：转换失败！
}
```

**关键流程**:
1. 从 `dataset.srs` 获取源 CRS（如 "EPSG:3857"）。
2. 从 `dataset.extent` 解析 extent 坐标。
3. 调用 `CrsTransformUtil.transformExtentToWgs84()` 转换 extent 到 EPSG:4326。
4. 如果转换成功：返回转换后的 extent + crs = sourceCrs。
5. 如果转换失败（返回 null）：返回原始 extent + crs = sourceCrs。

### 2. CrsTransformUtil.transformExtentToWgs84()

**文件**: `backend/.../CrsTransformUtil.java:32-35`

```java
try {
    CoordinateReferenceSystem source = CRS.decode(sourceCrs);
    CoordinateReferenceSystem target = CRS.decode(TARGET_CRS);
    // ...
} catch (Exception e) {
    LOGGER.log(Level.WARNING, "Failed to transform extent from " + sourceCrs + "...");
    return null;  // 转换失败返回 null
}
```

**CRS.decode() 要求**:
- 必须传入标准格式，如 `"EPSG:3857"`
- 如果传入 `"EPSG:WGS 84 / Pseudo-Mercator"` 或其他非标准格式，会抛出异常

### 3. GeoTiffParser.extractCrs()

**文件**: `backend/.../GeoTiffParser.java:167-230`

```java
private static String extractCrs(Object crsObj) {
    // 方法1: 尝试获取 identifiers
    java.lang.reflect.Method getIds = crsObj.getClass().getMethod("getIdentifiers");
    Object identifiers = getIds.invoke(crsObj);
    // ...
    
    // 关键代码!
    String idStr = id.toString();  // 这可能返回什么？
    if (idStr.startsWith("EPSG:")) {
        return idStr;  // 如果 id.toString() 返回 "EPSG:3857"
    }
    // Fallback
    try {
        java.lang.reflect.Method getCode = id.getClass().getMethod("getCode");
        String codeStr = (String) getCode.invoke(id);  // 可能返回 "3857"
        return "EPSG:" + codeStr;
    } catch (Exception e) {
    }
}
```

**问题**: `id.toString()` 可能返回的格式不确定，可能是：
- `"EPSG:3857"` ✓
- `"3857"` → 触发 fallback → 返回 `"EPSG:3857"` ✓
- `"urn:ogc:def:crs:EPSG::3857"` → 不以 "EPSG:" 开头 → fallback 可能处理

---

## 可能的问题场景

### 场景 1: extent 转换成功，但前端仍显示错误

**原因**: 前端代码逻辑问题

前端 `loadImageLayer()`:
```javascript
// extent 来自后端，可能已经是 EPSG:4326
let extent: any = imageInfo.extent

// 始终尝试根据 imageInfo.crs 转换！
if (imageInfo.crs && imageInfo.crs !== viewProjection) {
    if (imageInfo.crs === 'EPSG:3857' && viewProjection === 'EPSG:4326') {
        // 如果 crs 是 "EPSG:3857"，会执行转换
        const transformed = proj.transformExtent(extent, 'EPSG:3857', 'EPSG:4326')
        // 这里会再次转换！
    }
}
```

**问题**:
- 如果后端 `getImageWmsInfo()` 成功转换了 extent (EPSG:3857 → EPSG:4326)
- 但前端仍然根据 `crs: "EPSG:3857"` 再次转换 extent
- **EPSG:4326 → EPSG:4326 再次转换 = 错误的结果！**

### 场景 2: extent 转换失败（更可能的情况）

**原因**: `dataset.srs` 值不是标准格式

后端日志应该显示:
```
Failed to transform extent from EPSG:WGS 84 / Pseudo-Mercator to EPSG:4326: ...
```

然后返回:
```java
info.setExtent(extent);  // 原始 extent (EPSG:3857 米制坐标)
info.setCrs(sourceCrs);  // 原始 CRS
```

前端收到:
```javascript
imageInfo = {
    extent: [12930000, 4855000, 12940000, 4856000],  // EPSG:3857 米制坐标
    crs: "EPSG:WGS 84 / Pseudo-Mercator"  // 或其他非标准值
}
```

前端条件检查:
```javascript
if (imageInfo.crs === 'EPSG:3857' && viewProjection === 'EPSG:4326')
```
**失败!** 因为 `crs` 可能不等于 `"EPSG:3857"`！

结果: 不执行转换，直接 fit 使用 EPSG:3857 坐标到 EPSG:4326 视图 → 地图飞到海里！

---

## 诊断需要的信息

请在浏览器控制台和后端日志中检查以下内容：

### 1. 后端日志搜索

在应用日志中搜索图像 ID（如 27）的日志：
```
Image 27 extent transformation: sourceCRS=?, extent=[?, ?, ?, ?]
```

如果看到 `sourceCRS=EPSG:WGS 84 / Pseudo-Mercator` 或类似非标准值，说明 `fix-srs-epsg-code` 修复可能未生效。

### 2. 前端 Console.log 调试

在 `loadImageLayer` 函数添加调试：
```javascript
function loadImageLayer(imageInfo: ImageLayerInfo) {
  console.log('[loadImageLayer] extent:', imageInfo.extent)
  console.log('[loadImageLayer] crs:', imageInfo.crs)
  console.log('[loadImageLayer] viewProjection:', map.value.getView().getProjection().getCode())
  // ...
}
```

### 3. 数据库查询

```sql
SELECT id, name, srs, extent FROM dataset WHERE type = 'raster' LIMIT 5;
```

查看 `srs` 字段是否为 `"EPSG:3857"` 或其他值。

### 4. WMS 请求检查

在浏览器 Network 面板找到 WMS 请求，检查 URL 参数：
- `BBOX` 参数是什么值？

---

## 最可能的根因

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        最可能的根因                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  情况 A: fix-srs-epsg-code 修复未生效                                       │
│  ────────────────────────────────                                          │
│  • dataset.srs 仍然是非标准格式，如 "EPSG:WGS 84 / Pseudo-Mercator"        │
│  • CRS.decode() 失败                                                        │
│  • extent 转换返回 null                                                     │
│  • 后端返回原始 extent (EPSG:3857 米制坐标)                                 │
│  • 前端条件 imageInfo.crs === 'EPSG:3857' 不匹配                           │
│  • 不执行 extent 转换，直接 fit → 地图飞海里                               │
│                                                                             │
│  情况 B: 后端转换成功但前端重复转换                                          │
│  ─────────────────────────────────                                          │
│  • dataset.srs = "EPSG:3857" ✓                                             │
│  • 后端转换 extent 成功 (EPSG:3857 → EPSG:4326) ✓                          │
│  • 但返回 crs 仍然是 "EPSG:3857" (用于 WMS 请求)                           │
│  • 前端看到 crs="EPSG:3857"，再次转换 extent                               │
│  • EPSG:4326 → 转换 → 错误坐标                                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 修复方案

### 方案 1: 修正后端返回逻辑（推荐）

**关键修改**: 返回 `crs` 时使用转换后的目标 CRS 用于 extent 显示，但返回原始 CRS 用于 WMS。

但更简单的方案是：

**让前端始终信任后端返回的 extent 是正确的视图 CRS**：

```javascript
// 修改前端逻辑:
// 始终将后端返回的 extent 当作视图 CRS 使用，不再自动转换
// 只有在确认 crs 和 view 不匹配时才转换

if (imageInfo.crs && imageInfo.crs !== viewProjection) {
    // 但这里应该检查 extent 是否已经是目标 CRS
    // 而不是盲目转换
}
```

### 方案 2: 完全信任后端 extent

如果后端正确转换了 extent，前端应该直接使用，不要再次转换。

关键判断：**如果 `imageInfo.extent` 的值看起来像经纬度（约 -180 到 180, -90 到 90），就直接使用。**

```javascript
function isGeographicExtent(extent) {
    return extent[0] >= -180 && extent[0] <= 180 &&
           extent[1] >= -90 && extent[1] <= 90 &&
           extent[2] >= -180 && extent[2] <= 180 &&
           extent[3] >= -90 && extent[3] <= 90
}
```

---

## 下一步行动

请提供以下调试信息：

1. **后端日志**: 搜索 "extent transformation" 相关的日志输出
2. **数据库**: `SELECT id, srs, extent FROM dataset WHERE type = 'raster'`
3. **前端控制台**: 在地图加载影像时控制台的 `imageInfo` 输出

这样可以准确定位问题。
