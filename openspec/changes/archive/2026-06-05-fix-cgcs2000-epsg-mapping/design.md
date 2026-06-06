## Context

当前 CrsTransformUtil 中的 CHINA_CRS_MAPPINGS 已包含 CGCS2000 高斯-克吕格 3 度带投影坐标系的映射，但存在以下潜在问题：

1. **EPSG 代码历史问题**：旧代码曾使用 4491-4494 作为投影坐标系代码，现已更正为 4524-4533
2. **格式兼容性问题**：不同的 GIS 软件输出的 CRS 名称格式不同，需要同时支持下划线和斜杠格式
3. **完整性验证**：需要验证 Zone 35-40 的映射是否完整且正确

## Goals / Non-Goals

**Goals:**
- 确保 CGCS2000_3_Degree_GK_Zone_38 正确映射到 EPSG:4527
- 补充完整的 Zone 35-40 映射（EPSG:4524-4529）
- 支持多种 CRS 名称格式

**Non-Goals:**
- 不修改 GeoTools 库或 CRS.decode() 的行为
- 不添加新的坐标转换算法
- 不处理 6 度带投影坐标系

## Decisions

### Decision 1: EPSG 代码确认

**方案**：使用 EPSG:4524-4529 作为 CGCS2000 3 度带的官方代码

| 带号 | 中央经线 | EPSG 代码 |
|------|----------|-----------|
| Zone 35 | 105°E | 4524 |
| Zone 36 | 108°E | 4525 |
| Zone 37 | 111°E | 4526 |
| Zone 38 | 114°E | 4527 |
| Zone 39 | 117°E | 4528 |
| Zone 40 | 120°E | 4529 |

### Decision 2: 格式兼容性

**方案**：在同一映射表中支持下划线和斜杠两种格式

- 下划线格式：`CGCS2000_3_Degree_GK_Zone_38`
- 斜杠格式：`CGCS2000 / 3-degree Gauss-Kruger zone 38`

两者指向相同的 EPSG 代码。

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| 其他 Zone 号未覆盖 | 仅支持 35-40带，后续按需扩展 |
| 不同软件命名差异 | 使用 contains() 匹配而非精确匹配 |
