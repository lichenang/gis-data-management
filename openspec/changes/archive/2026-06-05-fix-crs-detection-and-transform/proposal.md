## Why

当前 Shapefile 导入流程存在坐标系识别问题，导致中国常用坐标系（CGCS2000/EPSG:4490）被错误处理。用户上传中国山西矢量数据后，地图上显示位置跑到国外（伊朗/波斯湾地区），实际坐标 (110°E~115°E, 34°N~40°N) 被误读为 (113.5°N, 36.2°E)。

根本原因：`CrsTransformUtil.getEpsgCode()` 无法识别中国坐标系名称（如 "GCS_China_Geodetic_Coordinate_System_2000"），返回 0 导致 `MultiFormatImportServiceImpl` 错误使用 EPSG:4326 作为源坐标系。

## What Changes

- 增强 `CrsTransformUtil`，添加中国常用坐标系名称到 EPSG 代码的静态映射表
- 改进 `CrsTransformUtil.getEpsgCode()` 方法，实现多级识别策略（identifier 提取 > 映射表匹配 > EPSG 字符串提取）
- 优化 `MultiFormatImportServiceImpl` 的坐标系处理逻辑，识别失败时根据情况回退或抛出明确异常
- 添加详细的 Slf4J 日志记录，便于问题诊断

## Capabilities

### New Capabilities

- `china-crs-detection`: 增强坐标系识别能力，支持 CGCS2000、Beijing 1954、Xian 1980 等中国常用坐标系

### Modified Capabilities

- `multi-format-vector-import`: 改进 Shapefile 导入时的坐标系转换逻辑，增加错误处理和日志

## Impact

### 受影响的文件

- `backend/src/main/java/com/gisplatform/util/CrsTransformUtil.java` - 核心修改
- `backend/src/main/java/com/gisplatform/service/impl/MultiFormatImportServiceImpl.java` - 核心修改

### 非目标

- 不修改现有的 extent 转换逻辑
- 不修改 GeoJSON 导入逻辑
- 不修改影像数据的坐标系处理
- 不添加新的 API 接口
