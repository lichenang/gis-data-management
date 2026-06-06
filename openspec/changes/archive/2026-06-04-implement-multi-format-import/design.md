## Context

基于 openspec/specs/multi-format-vector-support.md 规格实现多格式导入。

## Goals / Non-Goals

Goals:
1. 完成 Shapefile 导入 (.shp / .zip)
2. 修复 FormatDetector ZIP 读取
3. 优化前端提示

Non-Goals:
- 不添加新的导出格式

## Decisions

### 1. Shapefile 解析方案

使用 JTS GeometryBuilder 直接从 Shapefile 二进制读取：
- 读取 .shp 文件头获取几何类型
- 解析坐标数组构建 JTS Geometry
- 避免依赖 GeoTools DataStore (版本问题)

### 2. FormatDetector 修复

- 使用 ZipInputStream 二进制读取
- 不使用字符流读取 ZIP 条目名称

### 3. 前端提示

- 统一使用后端返回的 message
- 显示具体错误信息

## Migration Plan

1. 修复 FormatDetector (优先)
2. 实现 Shapefile 解析
3. 移除"暂未实现"硬编码
4. 前端优化

## Risks

Shapefile 解析复杂度较高，可能需要多次迭代。
