# 诊断报告：切片包下载路径问题

## 问题描述

切片包下载接口仍然报告 "该影像尚未生成切片缓存"，但 GWC 目录已确认存在：
- 实际路径：`D:\Program Files\geoserver-2.28.3-bin\data_dir\gwc\gisplatform_raster_38`

## 代码分析

### 1. 路径构造 (TilePackageServiceImpl.java:85-86)

```java
String gwcDirPath = dataDir.replace('\\', '/')
        + "/gwc/" + workspace + "_" + layerName;
```

**配置值** (application.yml:105):
```yaml
geoserver:
  data-dir: ${GEOSERVER_DATA_DIR:D:\Program Files\geoserver-2.28.3-bin\data_dir}
```

**构造路径**:
```
D:/Program Files/geoserver-2.28.3-bin/data_dir + /gwc/gisplatform_raster_38
= D:/Program Files/geoserver-2.28.3-bin/data_dir/gwc/gisplatform_raster_38
```

**结论**: 目录路径构造正确 ✓

---

### 2. 目录存在性检查 (TilePackageServiceImpl.java:88-91)

```java
File gwcDir = new File(gwcDirPath);
if (!gwcDir.exists() || !gwcDir.isDirectory()) {
    throw new RuntimeException("GeoServer data_dir 目录不存在: " + gwcDirPath);
}
```

这部分应该通过（因为目录存在）。

---

### 3. 瓦片枚举 (TilePackageServiceImpl.java:93-95)

```java
List<File> tileFiles = enumerateTileFiles(gwcDir, zoomStart, zoomStop, bounds);
if (tileFiles.isEmpty()) {
    throw new RuntimeException("该影像尚未生成切片缓存，请先触发切片种子任务");
}
```

**这是实际抛出错误的位置** - `enumerateTileFiles` 返回空列表。

---

### 4. 坐标范围计算分析

`enumerateTileFiles` 调用的关键逻辑：

#### 4.1 Bounds 来源 (parseBounds:173-214)

```
1. 请求参数 bounds → 直接使用
2. dataset.extent → 解析 JSON，转换为 WGS84
3. 以上都无 → 默认: Bounds(-180, -90, 180, 90) [全球范围]
```

#### 4.2 瓦片坐标计算 (getTileRange:149-163)

```java
int xMin = (int) Math.floor((bounds.minX + 180) / 360 * (1 << z));
int xMax = (int) Math.floor((bounds.maxX + 180) / 360 * (1 << z));
int yMin = (int) Math.floor(tileY(bounds.maxY, z));  // maxY → yMin
int yMax = (int) Math.floor(tileY(bounds.minY, z));  // minY → yMax
```

- **x**: 基于 longitude [-180, 180] → tile 范围
- **y**: 基于 latitude [-90, 90]，使用 Web Mercator 公式 (tileY)

公式本身是正确的 OSM/Google 瓦片编号方案。

---

## 根因分析

### 可能的问题

| # | 问题 | 可能性 | 说明 |
|---|------|--------|------|
| 1 | **YAML 路径解析** | ⚠️ 高 | `D:\Program Files\` 中的反斜杠可能在 YAML 解析时出现问题 |
| 2 | **dataset.extent 为空** | ⚠️ 高 | 回退到全球范围，但实际瓦片只覆盖影像区域 |
| 3 | **dataset.extent 值错误** | ⚠️ 中 | extent 与实际瓦片范围不匹配 |
| 4 | **tileY 公式问题** | ❌ 低 | 公式是标准的 Web Mercator，应该正确 |
| 5 | **Zoom 范围问题** | ⚠️ 中 | 默认 zoom: 0-14，需确认瓦片生成了哪些级别 |

---

### 详细分析

#### 问题 1: YAML 路径

application.yml 中的路径：
```yaml
data-dir: ${GEOSERVER_DATA_DIR:D:\Program Files\geoserver-2.28.3-bin\data_dir}
```

YAML 解析可能将 `\P` 视为转义序列。Java 读取后可能变成：
- 期望值：`D:\Program Files\geoserver-2.28.3-bin\data_dir`
- 实际值：`D:Program Files\geoserver-2.28.3-bin\data_dir` (反斜杠丢失)

这会导致 `new File(gwcDirPath)` 创建的路径不存在。

**验证方法**: 添加日志输出 `dataDir` 的完整值。

#### 问题 2 & 3: 范围问题

如果 dataset.extent 为空或与实际不符：
- 默认使用全球范围：`x: [0, 1], y: [0, 1]` (zoom 0)
- 但 GWC 可能只在特定区域生成了瓦片

#### 问题 5: Zoom 范围

代码默认 `zoomStart=0`, `zoomStop=14`：
```java
int zoomStart = request.getZoomStart() != null ? request.getZoomStart() : 0;
int zoomStop = request.getZoomStop() != null ? request.getZoomStop() : tilePackageProperties.getDefaultZoomStop(); // 14
```

如果瓦片只在 zoom 15+ 存在，则找不到。

---

## 建议诊断步骤

1. **添加调试日志** (TilePackageServiceImpl.java:60-86)

```java
log.debug("dataDir: {}", dataDir);
log.debug("gwcDirPath: {}", gwcDirPath);
log.debug("gwcDir exists: {}", gwcDir.exists());
log.debug("zoom range: {}-{}", zoomStart, zoomStop);
log.debug("bounds: minX={}, minY={}, maxX={}, maxY={}", 
    bounds.minX, bounds.minY, bounds.maxX, bounds.maxY);
log.debug("dataset extent from DB: {}", dataset.getExtent());
log.debug("dataset srs: {}", dataset.getSrs());
```

2. **检查数据库中 dataset 38 的 extent 和 srs 字段**

3. **检查 GWC 目录内实际文件结构**

```
gwcDir/
├── 0/
│   ├── 0/
│   │   └── 0.png
│   └── 1/
│       └── 0.png
├── 1/
...
```

4. **手动验证 tileY 计算**

zoom=0, lat=0:
```
y = (1 - log(tan(0) + 1/cos(0)) / π) / 2 * 1 = 0.5
```

---

## 结论

最可能根因：
1. **YAML 路径解析问题** - 需要添加日志验证
2. **Extent 为空或错误** - 需要检查数据库

建议优先添加详细日志定位问题。
