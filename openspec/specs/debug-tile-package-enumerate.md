# 诊断报告：enumerateTileFiles 返回空列表问题

## 问题定位

第 95 行抛出的异常：
```java
List<File> tileFiles = enumerateTileFiles(gwcDir, zoomStart, zoomStop, bounds);
if (tileFiles.isEmpty()) {
    throw new RuntimeException("该影像尚未生成切片缓存，请先触发切片种子任务");
}
```

这意味着 `enumerateTileFiles` 返回了空列表，尽管目录存在。

---

## enumerateTileFiles 执行流程详解

### 流程图

```
┌─────────────────────────────────────────────────────────────────────┐
│                     enumerateTileFiles 执行流程                      │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│  parseBounds │ ───▶ │ getTileRange │ ───▶ │enumerateTiles│
│  (获取范围)   │      │  (算行列范围)  │      │  (遍历文件)   │
└──────────────┘      └──────────────┘      └──────────────┘
       │                     │                     │
       ▼                     ▼                     ▼
  - 请求参数             - zoom=5             z=5 时:
  - dataset.extent     - xMin=22             x=22,y=14 → 5/22/14.png
  - 全球默认(-180..)   - xMax=28             x=22,y=15 → 5/22/15.png
                       - yMin=14             ...
                       - yMax=22

  共需检查: (28-22+1) * (22-14+1) = 7 * 9 = 63 个文件
```

---

## Step 1: parseBounds — 范围来源分析

**TilePackageServiceImpl.java:173-214**

| 优先级 | 来源 | 说明 |
|--------|------|------|
| 1 | `request.getBounds()` | API 请求参数，四个坐标必须都不为 null |
| 2 | `dataset.getExtent()` | 从数据库读取，JSON 格式 |
| 3 | **默认值** | `new Bounds(-180, -90, 180, 90)` 全球范围 |

### 关键代码 (lines 174-185)
```java
if (request.getBounds() != null
    && request.getBounds().getMinX() != null   // ← 必须全部非空
    && request.getBounds().getMaxX() != null
    && request.getBounds().getMinY() != null
    && request.getBounds().getMaxY() != null) {
    // 使用请求参数
}
```

### 回退逻辑 (lines 187-213)
```java
if (dataset.getExtent() != null && !dataset.getExtent().isEmpty()) {
    // 解析 dataset.extent (JSON)
    Map<String, Object> extentMap = mapper.readValue(dataset.getExtent(), Map.class);
    extent[0] = ((Number) extentMap.get("minX")).doubleValue();
    extent[1] = ((Number) extentMap.get("minY")).doubleValue();
    extent[2] = ((Number) extentMap.get("maxX")).doubleValue();
    extent[3] = ((Number) extentMap.get("maxY")).doubleValue();
    
    // 如果 dataset.getSrs() 不是 EPSG:4326，转换为 WGS84
    if (!"EPSG:4326".equalsIgnoreCase(sourceCrs)) {
        transformed = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);
    }
}

/** ⚠️ 如果 dataset.getExtent() 为空或解析失败 **/
return new Bounds(-180, -90, 180, 90);  // 回退到全球范围!
```

---

## Step 2: getTileRange — 瓦片坐标计算

**TilePackageServiceImpl.java:149-163**

### 公式

```java
int xMin = (int) Math.floor((bounds.minX + 180) / 360 * (1 << z));
int xMax = (int) Math.floor((bounds.maxX + 180) / 360 * (1 << z));

// tileY 使用 Web Mercator 投影公式
int yMin = (int) Math.floor(tileY(bounds.maxY, z));  // maxY → yMin (北方)
int yMax = (int) Math.floor(tileY(bounds.minY, z));  // minY → yMax (南方)
```

### 关键：坐标含义

```
传统 GIS 坐标:          Web Mercator 瓦片编号:
  minX = 经度最小值      x 向东增加
  maxX = 经度最大值      
  minY = 纬度最小值      y 向南增加 (⚠️ 与传统相反!)
  maxY = 纬度最大值      tileY(北纬) < tileY(南纬)
```

### 例子：假设 extent = 中国区域
- bounds.minX = 73 (经度)
- bounds.maxX = 135
- bounds.minY = 3 (纬度)
- bounds.maxY = 54

**Zoom = 14 (2^14 = 16384)**:
```
xMin = floor((73+180)/360*16384) = floor(11459)   = 11459
xMax = floor((135+180)/360*16384) = floor(14310)  = 14310

yMin = floor(tileY(54°))  = floor(5848)  = 5848
yMax = floor(tileY(3°))   = floor(10909) = 10909
```

遍历检查: (14310-11459+1) * (10909-5848+1) = 2852 * 5062 ≈ **1444万次查询!**

### 默认全球范围时的尴尬情况

```java
// bounds = (-180, -90, 180, 90)
xMin = 0, xMax = 1  // zoom 0
yMin = 0, yMax = 0  // 南北极被 clamp 到 0
```

---

## Step 3: 遍历查找瓦片文件

**TilePackageServiceImpl.java:130-143**

```java
private List<File> enumerateTileFiles(File gwcDir, int zoomStart, int zoomStop, Bounds bounds) {
    List<File> files = new ArrayList<>();
    for (int z = zoomStart; z <= zoomStop; z++) {
        TileRange range = getTileRange(z, bounds);
        for (int x = range.xMin; x <= range.xMax; x++) {
            for (int y = range.yMin; y <= range.yMax; y++) {
                File tileFile = new File(gwcDir, z + "/" + x + "/" + y + ".png");  // ⚠️ 关键假设!
                if (tileFile.exists() && tileFile.isFile()) {
                    files.add(tileFile);
                }
            }
        }
    }
    return files;
}
```

**核心假设**: GWC 瓦片路径结构为 `{z}/{x}/{y}.png`

---

## 可能的根因分析

### 根因 1: dataset.extent 为空或不正确 [⚠️ 高概率]

**症状**: 如果数据库中 dataset.extent 为 NULL，则回退到全球范围。

**分析**:
- 全球范围 (-180,-90,180,90) 在默认 zoom 0-14 时
- 只有极少量坐标点
- 如果实际 GWC 只在特定区域 (如中国) 生成瓦片，全球范围算出的瓦片坐标与实际不匹配

**验证**: 检查 dataset 38 的 extent 字段
```sql
SELECT id, name, extent, srs FROM dataset WHERE id = 38;
```

---

### 根因 2: CRS 转换问题 [⚠️ 中概率]

**CrsTransformUtil.java:42-55**

```java
double[] minPoint = new double[]{extent[0], extent[1]};  // [minX, minY]
double[] maxPoint = new double[]{extent[2], extent[3]};  // [maxX, maxY]

mathTransform.transform(minPoint, 0, minResult, 0, 1);
mathTransform.transform(maxPoint, 0, maxResult, 0, 1);

// ⚠️ 这里有坐标交换!
result[0] = minResult[1];  // lon → 把 y 放到 x
result[1] = minResult[0];  // lat → 把 x 放到 y
result[2] = maxResult[1];
result[3] = maxResult[0];
```

**问题**: 假设 extent 存储格式为 [lon, lat]，但 GeoTIFF 可能存储为 [lat, lon]。

---

### 根因 3: GWC 目录结构假设错误 [⚠️ 中概率]

代码假设瓦片路径: `5/22/14.png`

但 GWC 实际目录结构可能是:
```
gisplatform_raster_38/
├── EPSG_3857_5/
│   └── 0_0/
│       └── 5_22_14.png
└── ...
```

或者使用不同的文件名模式。

---

### 根因 4: Zoom 级别不匹配 [⚠️ 中概率]

```java
int zoomStart = request.getZoomStart() != null ? request.getZoomStart() : 0;
int zoomStop = request.getZoomStop() != null ? request.getZoomStop() : 14;
```

- 默认查询 zoom 0-14
- 但 GWC 可能只在 zoom 15-18 生成了瓦片
- 或者只生成了 zoom 6

---

### 根因 5: 数据路径解析问题 [⚠️ 需要验证]

application.yml 中:
```yaml
data-dir: ${GEOSERVER_DATA_DIR:D:\Program Files\geoserver-2.28.3-bin\data_dir}
```

YAML 中的 `\P` 可能会被解析为转义序列，导致路径错误。

---

## 推荐诊断方案

### 1. 立即添加日志输出

在 `TilePackageServiceImpl.java:78` 之后添加:

```java
log.info("===== TILE PACKAGE DEBUG =====");
log.info("dataset extent from DB: {}", dataset.getExtent());
log.info("dataset srs: {}", dataset.getSrs());
log.info("bounds: minX={}, minY={}, maxX={}, maxY={}, CRS=EPSG:4326", 
    bounds.minX, bounds.minY, bounds.maxX, bounds.maxY);
log.info("zoom range: {}-{}", zoomStart, zoomStop);
log.info("gwcDir: {}", gwcDir.getAbsolutePath());
log.info("gwcDir exists: {}", gwcDir.exists());
```

### 2. 检查数据库 dataset 38

```sql
SELECT 
    id, name, type, status, 
    extent, srs, 
    cache_seed_status, tile_progress
FROM dataset 
WHERE id = 38;
```

### 3. 检查 GWC 目录实际内容

```
实际目录: D:\Program Files\geoserver-2.28.3-bin\data_dir\gwc\gisplatform_raster_38
```

目录结构可能是:
```
gisplatform_raster_38/
├── 0/
│   ├── 0/
│   │   └── 0.png
│   └── 1/
│       └── 0.png
├── 1/
│   ├── 0/
│   │   ├── 0.png
│   │   └── 1.png
...
```

或者完全不同!

---

## 快速验证猜测

使用以下测试验证 extent 是否为根因：

```bash
# 调用 API 时明确传入影像实际覆盖范围
GET /api/v1/tiles/package/38?zoomStart=5&zoomStop=10
Content-Type: application/json

{
    "bounds": {
        "minX": 73,
        "minY": 3,
        "maxX": 135,
        "maxY": 54
    }
}
```

如果这样能返回瓦片，说明问题确实是 extent 为空或不正确。
