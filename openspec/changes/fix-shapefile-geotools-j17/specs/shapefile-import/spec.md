# Shapefile 导入功能规格说明

## 模块划分

- **数据解析层** (`MultiFormatImportService`): 负责 Shapefile 文件解析，使用 GeoTools ShapefefileDataStore
- **元数据提取**: 从 Shapefile 中提取几何类型、坐标系、边界框、属性字段等信息

## 数据流设计

```
用户上传 (.zip /.shp)
        │
        ▼
┌───────────────────┐
│ 编码检测 │ FormatDetector │  detectZipCharset
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│ 解压文件 │ extractShapefileFromZipToFile │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│ ShapefileDataStore│  getDataStore / parseShapefile
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│ 返回 GisDataParseResult │
└───────────────────┘
```

## 接口列表

| 接口 | 说明 |
|------|------|
| `parseShapefile(MultipartFile file)` | 解析 Shapefile 文件，返回元数据 |

## ADDED Requirements

### Requirement: Shapefile 导入在 Java 17 环境下正常运行

系统 SHALL 使用 ShapefileDataStore 正确解析 Shapefile 文件，并在 Java 17 环境下无模块访问异常。

#### Scenario: 成功解析 Shapefile ZIP 包
- **WHEN** 用户上传包含 .shp/.shx/.dbf/.prj 文件的 ZIP 包
- **THEN** 系统正确提取文件并解析，返回几何类型、坐标系、要素数量、边界框等信息

#### Scenario: 成功解析单个 Shapefile 文件
- **WHEN** 用户上传单个 .shp 文件
- **THEN** 系统正确解析文件，返回元数据信息

#### Scenario: Java 17 模块访问正常
- **WHEN** 在 Java 17 环境下执行 Shapefile 解析
- **THEN** 不出现 IllegalAccessError 或类似模块访问异常

#### Scenario: 正确检测坐标参考系统
- **WHEN** Shapefile 包含 .prj 文件
- **THEN** 系统正确解析 EPSG 代码（如 EPSG:4326）

#### Scenario: 处理编码问题
- **WHEN** 上传包含中文字段的 Shapefile
- **THEN** 系统能正确检测并处理字符编码

## MODIFIED Requirements

### Requirement: Shapefile 解析实现方式

以下内容是对 `multi-format-vector-support.md` 中相关规范的修改：

**修改内容：**
将 `FileDataStore` 实现改为 `ShapefileDataStore` 实现，使用 GeoTools 32.x 统一 API。

**原始描述 (multi-format-vector-support.md 第19行)：**
> Shapefile | `.shp` | `ShapefileDataStore` | 需同时上传 .shx, .dbf, .prj 等

**更新后：**
> Shapefile | `.shp`, `.zip` | `ShapefileDataStore` | 支持 ZIP 打包上传，Java 17 兼容

**技术细节更新：**
1. Import 语句统一使用 `org.geotools.api.*` 包
2. maven-compiler-plugin 添加 `--add-opens`/`--add-exports` 参数
3. 使用 `ShapefileDataStoreFinder.getDataStore()` 获取 DataStore 实例
