# 诊断报告：切片包下载返回 0B 空文件

## 逐行分析

### 环节 1：请求入口 — TilePackageController.java

```java
// 第 45-47 行：设置响应头
response.setContentType("application/zip");
response.setHeader("Content-Disposition", "attachment; filename=\"...\"");

// 第 49-61 行：处理请求和异常
try (OutputStream os = response.getOutputStream()) {   // ← ① 打开二进制流
    tilePackageService.packageTiles(id, request, os);
    os.flush();
} catch (Exception e) {
    if (!(e instanceof ClientAbortException)) {
        response.setStatus(500);                        // ← ② 尝试改状态码
        response.setContentType("application/json;..."); // ← ③ 尝试改 Content-Type
        try {
            response.getWriter().write("{\"code\":500,\"message\":\"...\"}"); // ← ④ 尝试写 JSON
        } catch (Exception ignored) {}
    }
}
```

**⚠️ Bug #1（严重）：混合使用 `getOutputStream()` 和 `getWriter()`**

| 行 | 调用 | 问题 |
|----|------|------|
| 49 | `response.getOutputStream()` | 打开二进制输出流 |
| 57 | `response.getWriter()` | Servlet API 禁止混用 getOutputStream/getWriter，抛出 `IllegalStateException` |

当 `packageTiles()` 在写入 ZIP 数据之前抛出异常时：
- 响应未提交（无数据写入）
- Catch 块试图写 JSON 错误 → `getWriter()` 抛出 `IllegalStateException`
- `catch (Exception ignored)` 静默吞噬异常
- 响应以原始头（`Content-Type: application/zip`）发送，body 为 **0 字节**

**影响链路：**

```
抛出异常 → ZipOutputStream 未创建 / 无数据写入
  → response 未提交
    → catch 块: getWriter() → IllegalStateException → silently ignored
      → 浏览器收到 Content-Type: application/zip, Content-Length: 0 → 0B 文件
```

### 环节 2：服务入口 — TilePackageServiceImpl.packageTiles()

```java
// 第 40-84 行：各种前置校验
Dataset dataset = datasetService.getById(datasetId);   // 检查数据集存在
if (!"published".equals(dataset.getStatus()))           // 检查已发布
if (!"seeded".equals(dataset.getCacheSeedStatus()))     // 检查切片完成
String dataDir = geoServerProperties.getDataDir();       // 检查 dataDir 配置
if (dataDir == null || dataDir.isEmpty())               // 未配置则抛出异常
// ...
if (!gwcDir.exists() || !gwcDir.isDirectory())          // 检查 GWC 目录存在
```

前置校验链正常。如果任何一项失败，抛出 RuntimeException → 回到 Bug #1（getOutputStream/getWriter 冲突 → 0B）。

### 环节 3：瓦片路径拼装

```java
// 第 78-79 行
String gwcDirPath = dataDir.replace('\\', '/')
        + "/gwc/" + workspace + "_" + layerName;
```

对于 ID=38 的数据集：
- `workspace` = `"gisplatform"`（从 application.yml）
- `layerName` = `"raster_38"`
- 结果路径 = `{dataDir}/gwc/gisplatform_raster_38/`

**GWC 目录名约定：** GWC 内部用 `{workspace}:{layerName}` 标识图层，磁盘目录将 `:` 替换为 `_`。所以 `gisplatform:raster_38` → `gisplatform_raster_38`。✅ 与用户确认的目录名一致。

### 环节 4：瓦片范围计算 — getTileRange()

```java
// 第 134-148 行
private TileRange getTileRange(int z, Bounds bounds) {
    int xMin = (int) Math.floor((bounds.minX + 180) / 360 * (1 << z));
    int xMax = (int) Math.floor((bounds.maxX + 180) / 360 * (1 << z));
    int yMin = (int) Math.floor(tileY(bounds.maxY, z));
    int yMax = (int) Math.floor(tileY(bounds.minY, z));
    // 夹紧到有效范围 [0, 2^z - 1]
}
```

Web Mercator 瓦片 Y 公式：

```java
// 第 150-153 行
private double tileY(double lat, int z) {
    double latRad = Math.toRadians(lat);
    return (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * (1 << z);
}
```

**⚠️ Bug #2：bounds 的 CRS 假设**

`bounds` 来源于 `parseBounds()` 方法（第 158-187 行），该方法从 `dataset.getExtent()` 直接读取 `minX/minY/maxX/maxY`，**不做任何 CRS 转换**。

但 `dataset.extent` 是在 `ImageServiceImpl.publishImageDataset()` （第 276-292 行）中直接从 `raster_metadata.transform` 写入的：

```java
// ImageServiceImpl.java 第 282-286 行
Double minX = ((Number) transform.get("minX")).doubleValue();  // 可能为 200000 (EPSG:32650)
Double minY = ((Number) transform.get("minY")).doubleValue();  // 可能为 3000000
Double maxX = ((Number) transform.get("maxX")).doubleValue();
Double maxY = ((Number) transform.get("maxY")).doubleValue();
String extentJson = String.format("{\"minX\":%s,\"minY\":%s,\"maxX\":%s,\"maxY\":%s}", ...);
```

对比 `getImageWmsInfo()`（第 394-431 行）中显式将 extent 从源 CRS 转换到 EPSG:4326：

```java
// ImageServiceImpl.java 第 414 行 — WMS 信息中正确转换了 CRS
double[] transformedExtent = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);
info.setExtent(transformedExtent);
info.setCrs("EPSG:4326");
```

**`TilePackageServiceImpl.parseBounds()` 缺失了同样的 CRS 转换步骤。**

**当源 CRS 是投影坐标系（如 EPSG:32650 UTM zone 50N）时：**

```
dataset.extent = {"minX":200000, "minY":3000000, "maxX":300000, "maxY":3100000}

代入 Web Mercator 公式（EPSG:4326 度）：
  xMin = floor((200000 + 180) / 360 * 2^z)  ← 200000 是米，非度
       = floor(200180 / 360 * 2^z)
       = floor(556.06 * 2^z)
  z=10: xMin = floor(556.06 * 1024) = 569,402  ← 远超有效范围 0-1023
  → 夹紧到 1023
  
  y: tileY(3000000, 10) — 3000000 米不是有效纬度
  → NaN 或极端值 → 夹紧到边界
```

**结果：** 所有坐标被夹紧到 `xMin=xMax=1023, yMin=yMax=1023`，只检查一个不存在的瓦片 `10/1023/1023.png` → 找不到 → `totalWritten == 0`。

### 环节 5：文件遍历

```java
// 第 89-105 行
for (int z = zoomStart; z <= zoomStop; z++) {
    TileRange range = getTileRange(z, bounds);       // 范围已是夹紧后的边界值
    for (int x = range.xMin; x <= range.xMax; x++) {
        for (int y = range.yMin; y <= range.yMax; y++) {
            File tileFile = new File(gwcDir, z + "/" + x + "/" + y + ".png");
            if (tileFile.exists() && tileFile.isFile()) {
                // 写入 ZIP (永远不会执行)
            }
        }
    }
}
```

无 `Files.walk()` 或目录扫描——采用的是**主动计算坐标后逐文件检查**模式。只要坐标计算正确，这是 O(1) 内存的高效方案。但在坐标错误时，所有文件都找不到。

### 环节 6、7、8：ZipOutputStream / 关闭 / 清空

```java
// 第 86-116 行
try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
    // ... 遍历 (无瓦片写入) ...
    if (totalWritten == 0) {                          // 我们的修复
        throw new RuntimeException("该影像尚未生成切片缓存...");  // ← 在 zos.close() 之前抛出!
    }
}
```

`throw new RuntimeException()` 在 try-with-resources 块内执行时：
1. JVM 先调用 `zos.close()` → 写入 ZIP 中央目录（最小空 ZIP ~22 字节）→ 关闭 `outputStream`
2. RuntimeException 传播到 Controller
3. Controller catch 块 → `getWriter()` 失败 → **0B 响应**

---

## 诊断结论

| # | Bug | 根因 | 严重程度 |
|---|-----|------|---------|
| 1 | **Controller 混合 getOutputStream/getWriter** | 错误处理路径调用 `response.getWriter()`，但主路径已调用 `getOutputStream()`，导致 `IllegalStateException` 被静默吞噬，返回 0B 空响应 | 🔴 阻塞 |
| 2 | **Tile 坐标计算使用的 extent 未做 CRS 转换** | `parseBounds()` 直接使用 `dataset.extent` 的原始值（可能在投影 CRS 中），不做 EPSG:4326 转换，导致计算出的瓦片坐标全部越界，找不到任何文件 | 🔴 阻塞 |
| 3 | **Fallback extent 为全幅 180°，可能不匹配实际数据范围** | 当 extent 解析失败时回退 `(-180, -90, 180, 90)`，坐标正确但实际 GWC 瓦片只覆盖局部区域 | 🟡 次要 |

### Bug 数据流

```
ImageServiceImpl.publishImageDataset()
  │
  ├─ raster_metadata.transform → {minX:200000, minY:3000000, ...}  ← UTM meters
  │
  └─ dataset.extent = {minX:200000, minY:3000000, maxX:300000, maxY:3100000}
                                                                    ↓
TilePackageServiceImpl.packageTiles()
  │
  ├─ parseBounds() → Bounds(200000, 3000000, 300000, 3100000)      ← ❌ 未转换 CRS
  │
  ├─ getTileRange(z=10) → (xMin:1023, yMin:1023, xMax:1023, yMax:1023)  ← 越界夹紧
  │
  ├─ 检查 10/1023/1023.png → 不存在 → totalWritten = 0
  │
  ├─ throw RuntimeException("尚未生成切片缓存...")
  │
  └─ Controller: getWriter() → IllegalStateException → 0 字节响应
```

---

## 修复方案

### 修复 1：Controller 统一使用 getOutputStream 写错误响应

将 catch 块中的 `response.getWriter().write(...)` 改为通过 `getOutputStream()` 写入 JSON 字节：

```java
catch (Exception e) {
    if (!(e instanceof ClientAbortException)) {
        response.reset();  // 重置头信息和状态码
        response.setStatus(500);
        response.setContentType("application/json;charset=UTF-8");
        String json = "{\"code\":500,\"message\":\"" + escapeJson(e.getMessage()) + "\",\"data\":null}";
        try (OutputStream os = response.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }
}
```

### 修复 2：parseBounds 增加 CRS 转换

在 `TilePackageServiceImpl.parseBounds()` 中，当从 `dataset.getExtent()` 读取后，根据 `dataset.getSrs()` 转换到 EPSG:4326：

```
parseBounds(TilePackageRequest request, Dataset dataset):
  ├─ 如果 request.bounds 存在 → 直接使用（假设前端传 WGS84）
  ├─ 否则从 dataset.extent 读取
  │     ├─ 获取 dataset.srs (源 CRS)
  │     ├─ 如果 srs != "EPSG:4326" → 调用 CrsTransformUtil.transformExtentToWgs84(extent, srs)
  │     └─ 如果转换失败 → 回退到 (-180, -90, 180, 90)
  └─ 返回 EPSG:4326 的 Bounds
```

### 修复 3（提前防御）：GWC 目录预检

在遍历瓦片之前，先检查 GWC 目录下是否存在至少一个 `.png` 文件。如果目录为空（或所有 zoom 级别目录都不存在），提前返回明确错误，避免走完整的 3 层循环。

```
if (!hasAnyPngFile(gwcDir)) {
    throw new RuntimeException("该影像尚未生成瓦片缓存文件");
}
```

---

## 总结

| 问题 | 优先级 | 修复范围 | 工作量 |
|------|--------|---------|--------|
| Bug #1: getOutputStream/getWriter 冲突 | P0 | `TilePackageController.java` | 5 分钟 |
| Bug #2: extent CRS 未转换 | P0 | `TilePackageServiceImpl.java` | 30 分钟 |
| 改进 #3: 目录空预检 | P1 | `TilePackageServiceImpl.java` | 10 分钟 |
