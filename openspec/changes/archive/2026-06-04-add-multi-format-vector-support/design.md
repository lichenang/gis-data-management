## Context

当前系统仅支持 GeoJSON 格式的矢量数据导入导出。用户需要导入 Shapefile（GIS 最常用格式）、KML/KMZ（Google Earth）、GPX（GPS 数据）、GML（OGC 标准）等格式。同时也需要支持多格式导出。

**现有实现问题：**
- `GisDataParserServiceImpl` 手动解析 JSON，代码难以维护
- 未使用 GeoTools 的 DataStore 抽象
- 前端仅接受 geojson/json 文件
- 导出功能虽有声明但实现不完整

**约束条件：**
- 必须保持与现有 PostGIS 数据库的兼容性
- 需要利用项目已有的 GeoTools 32.x 依赖
- 支持前后端分离架构

## Goals / Non-Goals

**Goals:**
1. 使用 GeoTools DataStore 统一解析多种矢量格式
2. 支持 8+ 种主流矢量格式的导入
3. 支持 4 种格式的导出（GeoJSON、Shapefile、KML、CSV）
4. 前端上传组件支持多格式选择
5. 实现 Shapefile zip 包和多文件上传
6. 自动坐标系转换

**Non-Goals:**
- 栅格数据格式扩展
- 数据库批量格式转换
- 实时矢量切片渲染为其他格式
- WFS 等网络服务支持

## Decisions

### 1. 使用 GeoTools DataStore 统一入口

**决定**: 使用 GeoTools 的 `DataStore` 接口作为统一的矢量数据读取抽象。

**理由**:
- GeoTools 已支持所有主流矢量格式（Shapefile、KML、GML、GPX、CSV 等）
- DataStore 提供统一的 API（getSchema, getFeatures, getTypeNames）
- 项目已引入 gt-shapefile、gt-main 等依赖
- 未来扩展新格式只需添加对应模块

**替代方案考虑**:
- 手动实现每种格式解析：工作量大，难以维护 ✓ 否定
- 使用 GDAL/OGR：功能强大但引入复杂依赖 ✓ 否定

### 2. 格式自动识别策略

**决定**: 使用文件扩展名 + 内容检测的组合策略。

```java
// 优先级：
// 1. 根据扩展名识别（.shp, .kml, .gpx 等）
// 2. 对于 .json/.geojson 文件，读取前 4KB 检测 type 字段
// 3. 对于 .zip 文件，内部搜索 .shp 文件确定类型
```

**理由**: 扩展名最快速，内容检测更可靠。

### 3. Shapefile 处理方案

**决定**: 支持两种上传方式：
- Zip 包上传：包含 .shp/.shx/.dbf/.prj 的 zip 文件
- 多文件上传：通过前端选择多个文件

**理由**:
- Zip 是 Shapefile 分发的标准方式
- 有些用户习惯单独选择文件

**技术实现**:
```java
if (filename.endsWith(".zip")) {
    // 解压到临时目录，查找 .shp 文件
    ZipInputStream zis = new ZipInputStream(file.getInputStream());
    // 解压后使用 ShapefileDataStoreFactory 创建 DataStore
} else {
    // 多文件情况：从 request 中获取其他文件
    // 或使用 ShapefileDataStore(file.getInputStream())
}
```

### 4. 坐标系转换

**决定**: 使用 GeoTools `CRS` 和 `ReferencedEnvelopeTransform` 进行坐标转换。

**理由**:
- GeoTools 内置丰富的坐标转换支持
- 可以自动选择最佳转换路径
- 对于中国常用坐标系（EPSG:4490）需特殊处理

**注意**: EPSG:4326 与 EPSG:4490 (CGCS2000) 的转换需要高精度的坐标转换参数，暂使用简化转换。

### 5. PostGIS 写入策略

**决定**: 使用 JDBC 直接写入，不通过 GeoTools PostGISDataStore（后者是读取用的）。

**理由**:
- 保持与现有代码一致
- 更细粒度的控制（批量插入、事务）
- 方便动态创建表结构

```java
// 批量插入，每 1000 条提交一次
conn.setAutoCommit(false);
for (int i = 0; i < features.size(); i++) {
    stmt.addBatch();
    if (i % 1000 == 0) {
        stmt.executeBatch();
        conn.commit();
    }
}
```

### 6. 前端组件设计

**决定**: 扩展现有 el-upload 组件，保留单文件上传逻辑，但扩展 accept 属性和提示信息。

**理由**:
- 减少 UI 改动量
- 保持用户体验一致

**accept 属性扩展**:
```
.geojson,.json,.shp,.zip,.kml,.kmz,.gml,.gpx,.csv,.wkt,.topojson
```

## Risks / Trade-offs

### [风险] GeoTools 内存占用

**描述**: 处理大型 Shapefile 时可能 OOM

**缓解**:
- 使用 GeoTools 的 `FeatureIterator` 流式处理
- 批量提交减少内存占用
- Spring Boot 配置最大文件上传 500MB

### [风险] 编码问题

**描述**: Shapefile 的 .dbf 文件编码可能是 GBK 或 UTF-8

**缓解**:
- 从 .cpg 文件读取编码声明
- 默认尝试 GBK，失败回退 UTF-8

### [风险] 坐标系识别

**描述**: 部分格式（如 CSV）不包含坐标系信息

**缓解**:
- CSV 要求用户指定坐标系
- 使用 EPSG:4326 作为默认值

### [风险] 破坏现有功能

**描述**: 修改 GisDataParserServiceImpl 可能影响现有 GeoJSON 导入

**缓解**:
- 保留原有 GeoJSON 解析逻辑作为回退
- 先在测试环境验证
- 准备回滚方案

### [权衡] TopoJSON 支持

**描述**: TopoJSON 不是 GeoTools 原生支持格式

**权衡**: 需要先转换为 GeoJSON 再处理，增加一步转换逻辑

**决定**: 导入时识别 TopoJSON，使用 topojson-java 库转换后处理

## Migration Plan

### 阶段 1: 后端核心实现
1. 创建 `VectorFileFormat` 枚举
2. 实现 `FormatDetector` 格式识别
3. 实现 `VectorDataStoreFactory` 工厂类
4. 改造 `MultiFormatImportService` 使用 DataStore

### 阶段 2: 导出功能
1. 实现 `MultiFormatExportService`
2. 扩展 ExportController CSV 格式

### 阶段 3: 前端
1. 扩展上传组件 accept
2. 更新 UI 提示信息

### 阶段 4: 测试
1. 各格式导入测试
2. 导出文件验证
3. 大文件性能测试

### 回滚方案
- 使用 Git 回滚代码
- 已导入数据需要手动清理（提供清理脚本）
