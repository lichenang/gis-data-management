## Context

当前 Shapefile 导入功能使用自定义 ByteBuffer 解析 .shp 二进制格式，经过多次修复偏移量问题仍无法正确读取几何数据。GeoTools 32.x 提供了标准化的 ShapefileDataStore API，可以可靠地读取 Shapefile。

## Goals / Non-Goals

**Goals:**
1. 使用 GeoTools ShapefileDataStore 替换手写二进制解析
2. 保证 Shapefile 导入功能正确工作（Point/LineString/Polygon）
3. 减少代码复杂度，提高可维护性

**Non-Goals:**
- 不修改 API 接口
- 不改变 GeoJSON 导入逻辑
- 不添加新的导入格式

## Decisions

### 1. 使用 ShapefileDataStore 而非 ShapefileReader

**选择方案：ShapefileDataStore**
- GeoTools 官方 API，稳定可靠
- 自动处理各种 Shapefile 变体（多维坐标、NULL 几何等）
- 支持从 File 或 URL 创建

**备选方案：ShapefileReader**
- 更底层，需要手动管理资源
- 实现更复杂

### 2. 如何处理 ZIP 中的 .shp 文件

**方案：临时文件**
- 从 ZIP 解压 .shp 文件到临时目录
- 使用临时文件创建 ShapefileDataStore
- 处理完成后清理临时文件

### 3. 属性处理

**方案：只存储核心属性**
- 对于简单表结构，存储所有属性
- 属性名 sanitize 后写入 PostGIS
- 几何列统一为 "geometry"

## Risks / Trade-offs

- **临时文件性能**: 解压到临时文件有 IO 开销 → Mitigation: 对于小文件可忽略，后续可优化为内存流
- **临时文件清理**: 需要确保异常情况下也清理 → Mitigation: 使用 try-with-resources 或 finally 块清理
