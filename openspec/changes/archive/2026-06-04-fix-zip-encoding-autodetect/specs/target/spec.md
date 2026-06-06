# 多格式矢量导入 - 编码自动检测

## ADDED Requirements

### Requirement: ZIP 文件编码自动检测
FormatDetector SHALL support automatic charset detection for ZIP file entries, trying multiple common charsets (UTF-8, GBK, GB18030, ISO-8859-1) to handle ZIP files from different sources.

#### Scenario: UTF-8 encoded ZIP
- **WHEN** Upload a ZIP file with UTF-8 encoded entry names
- **THEN** FormatDetector SHALL correctly detect and list all entries

#### Scenario: GBK encoded ZIP
- **WHEN** Upload a ZIP file exported from Chinese GIS software with GBK encoded entry names
- **THEN** FormatDetector SHALL fallback to GBK and correctly detect entries

#### Scenario: GB18030 encoded ZIP
- **WHEN** Upload a ZIP file with GB18030 encoded entry names
- **THEN** FormatDetector SHALL fallback to GB18030 and correctly detect entries

### Requirement: MultiFormatImportService ZIP 解压编码处理
MultiFormatImportService SHALL use the same charset detection logic when extracting .shp files from ZIP archives to ensure correct filename handling.

#### Scenario: Extract from UTF-8 ZIP
- **WHEN** Extract .shp file from a UTF-8 encoded ZIP
- **THEN** The extracted file SHALL have correct filename, no encoding errors

#### Scenario: Extract from GBK ZIP
- **WHEN** Extract .shp file from a GBK encoded ZIP
- **THEN** The extracted file SHALL have correct filename, no encoding errors

### Requirement: 无头编码的 ZIP 处理
System SHALL handle ZIP files without explicit encoding header gracefully.

#### Scenario: ZIP without encoding header
- **WHEN** Upload a ZIP file without encoding information in its header
- **THEN** System SHALL try multiple charsets until successful extraction

## 模块划分

### FormatDetector
- 职责：检测上传文件的格式，识别 Shapefile、KML 等矢量格式
- 依赖：VectorFileFormat 枚举
- 提供接口：
  - `detect(MultipartFile file, String filename)` - 自动识别文件格式
  - `detectCharset(InputStream is)` - 检测 ZIP 文件编码

### MultiFormatImportService
- 职责：解析多种格式的矢量数据并导入 PostGIS
- 依赖：FormatDetector、DatasetMapper、DataSource
- 修改方法：
  - `parseShapefile(MultipartFile file)` - 添加编码检测逻辑
  - `extractShapefileFromZip(MultipartFile file)` - 修复编码处理

## 数据流设计

```
用户上传 (.zip)
    ↓
FormatDetector.detect()
    ↓ (检测到 ZIP)
FormatDetector.detectCharset() → 尝试多种编码
    ↓
返回检测到的编码
    ↓
MultiFormatImportService.extractShapefileFromZip()
    ↓ (使用检测到的编码)
返回 .shp 文件的 InputStream
    ↓
parseShpFile() → 解析二进制
    ↓
insertShapefileRecords() → 写入 PostGIS
```

## 接口列表

### 现有接口不受影响
- `POST /api/v1/datasets/parse` - 文件解析（修改内部实现）
- `POST /api/v1/datasets/import` - 数据导入（修改内部实现）

### 新增方法

| 方法 | 所在类 | 说明 |
|------|--------|------|
| `detectCharset(InputStream)` | FormatDetector | 静态方法，检测 ZIP 编码 |
| `extractShapefileFromZip(MultipartFile)` | MultiFormatImportService | 带编码检测的解压方法 |
