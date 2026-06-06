## Why

当 Shapefile 或其他空间数据使用 CGCS2000 高斯-克吕格投影坐标系时，如果 CRS 名称格式不匹配，系统可能无法正确识别对应的 EPSG 代码，导致坐标转换失败或使用错误的坐标系。

## What Changes

1. **修正 CGCS2000_3_Degree_GK_Zone_38 的 EPSG 映射**：原本可能被错误映射到 EPSG:4490（地理坐标系），现修正为 EPSG:4527（投影坐标系）
2. **补充完整的 3 度带映射**：增加 EPSG:4524-4529 对应的 Zone 35-40 映射
3. **增强格式兼容性**：同时支持下划线格式（如 `CGCS2000_3_Degree_GK_Zone_38`）和斜杠格式（如 `CGCS2000 / 3-degree Gauss-Kruger zone 38`）

## Capabilities

### New Capabilities

- `cgcs2000-epsg-mapping`: 完善 CrsTransformUtil 中 CGCS2000 高斯-克吕格投影坐标系的 EPSG 映射规则

### Modified Capabilities

- `china-crs-detection`: 增强坐标系识别逻辑，支持更多 CGCS2000 投影变体

## Impact

### 受影响的文件

- `backend/src/main/java/com/gisplatform/util/CrsTransformUtil.java`

### 非目标

- 不修改坐标转换的核心算法
- 不添加新的坐标系类型
- 不修改数据库存储格式
