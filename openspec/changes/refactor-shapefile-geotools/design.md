## Context

当前 MultiFormatImportService 使用手写 ByteBuffer 解析 Shapefile 二进制格式。问题：多次调整记录头偏移量仍无法读取正确的 shape type，导致所有几何数据解析失败。

项目已有 GeoTools 32.x 依赖，提供标准 ShapefileDataStore API。

## Goals / Non-Goals

**Goals:**
1. 使用 GeoTools ShapefileDataStore 替换手写二进制解析
2. 清理未使用的 import
3. 确保 Shapefile 导入功能正常工作

**Non-Goals:**
- 不修改 API 接口
- 不改变 GeoJSON 逻辑

## Decisions

### 直接上传 .shp 文件
- 直接用 FileDataStoreFinder.getDataStore(File) 打开

### ZIP 中的 .shp 文件
- 提取 .shp + 同名 .shx/.dbf 到临时目录
- 用临时文件创建 DataStore
- 使用后清理临时文件

## Risks / Trade-offs

- 临时文件需要正确清理 → 使用 finally 块确保清理
