# 诊断报告：ZIP 包上传返回"不支持的格式: zip"

## 问题现象

用户上传 Shapefile ZIP 包后，系统返回错误：`不支持的格式: zip`

## 调用链追踪

```
DatasetController.parseFile() / importDataset()
    │
    ▼
DatasetServiceImpl.parseUploadFile() / importDataset()
    │
    ▼
GisDataParserServiceImpl.parseFile()    ← 问题位置
    │
    ▼
"不支持的格式: zip"
```

## 详细调用路径

| 步骤 | 文件 | 方法 | 行号 | 说明 |
|------|------|------|------|------|
| 1 | DatasetController.java | `parseFile()` | 72 | REST 入口 |
| 2 | DatasetController.java | `importDataset()` | 91 | REST 入口 |
| 3 | DatasetServiceImpl.java | `parseUploadFile()` | 132 | 委托 GisDataParserService |
| 4 | DatasetServiceImpl.java | `importDataset()` | 138 | 委托 GisDataParserService |
| 5 | GisDataParserServiceImpl.java | `parseFile()` | 53-68 | **抛出错误的位置** |

## 根本原因分析

`GisDataParserServiceImpl.parseFile()` 方法（第 53-73 行）的逻辑：

```java
public GisDataParseResult parseFile(MultipartFile file, String fileName) {
    try {
        String ext = getFileExtension(fileName);  // ext = "zip"
        if (!isSupportedFormat(ext)) {            // isSupportedFormat("zip") = true (zip 在列表中)
            return GisDataParseResult.error("不支持的文件格式: " + ext + "...");
        }

        String content = new String(file.getBytes(), "UTF-8");

        if ("geojson".equalsIgnoreCase(ext) || "json".equalsIgnoreCase(ext)) {
            return parseGeoJSON(content);
        } else if ("shp".equalsIgnoreCase(ext)) {
            return parseShapefileMetadata(file);
        }

        return GisDataParseResult.error("不支持的格式: " + ext);  // ← 第 68 行：永远执行到这里
    } catch (Exception e) {
        logger.error("解析文件失败", e);
        return GisDataParseResult.error("解析文件失败: " + e.getMessage());
    }
}
```

**问题**：`SUPPORTED_FORMATS` 数组（第 36-38 行）包含 `"zip"`：

```java
private static final String[] SUPPORTED_FORMATS = {
    "shp", "zip", "geojson", "json"
};
```

但 `parseFile()` 方法只处理 `geojson/json` 和 `shp` 格式，**没有处理 `zip` 格式**。因此代码流到达第 68 行返回错误。

## 两处错误消息的区别

| 位置 | 错误消息 | 触发条件 |
|------|----------|----------|
| GisDataParserServiceImpl.java:57 | `不支持的文件格式: xxx`，格式列表 | `isSupportedFormat(ext)` 返回 false |
| GisDataParserServiceImpl.java:68 | `不支持的格式: xxx` | 扩展名在支持列表中但不是 geojson/json/shp |
| VectorDataStoreFactory.java:48 | `不支持的格式: xxx` | switch-case 的 default 分支 |

## `importDataset` 路径

对于 `/api/v1/datasets/import` 接口，流程不同：

```
DatasetController.importDataset()  →  line 106
    │
    ▼
DatasetServiceImpl.importDataset()  →  line 138
    │
    ▼
GisDataParserServiceImpl.importToPostGIS()  →  line 178
    │
    ▼
VectorDataStoreFactory.createDataStore()  →  如果 format 被正确识别为 SHAPEFILE
```

`importToPostGIS()` 方法（第 178-231 行）同样检查 `isSupportedFormat(ext)`，如果 ext 是 `zip` 会通过检查，但同样只处理 geojson/json：

```java
if ("geojson".equalsIgnoreCase(ext) || "json".equalsIgnoreCase(ext)) {
    String content = new String(file.getBytes(), "UTF-8");
    importedCount = importGeoJSON(conn, content, tableName, datasetName);
} else {
    return DatasetImportResult.error("Shapefile 导入功能暂未实现，请使用 GeoJSON 格式");  // line 199
}
```

## 架构图

```
                    ┌─────────────────────────────┐
                    │   DatasetController         │
                    │  /api/v1/datasets/parse     │
                    │  /api/v1/datasets/import    │
                    └──────────────┬──────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │   DatasetServiceImpl        │
                    │  parseUploadFile()          │
                    │  importDataset()            │
                    └──────────────┬──────────────┘
                                   │
           ┌───────────────────────┴───────────────────────┐
           │                                               │
           ▼                                               ▼
┌─────────────────────────────┐           ┌─────────────────────────────┐
│  GisDataParserServiceImpl   │           │  GisDataParserServiceImpl   │
│  parseFile()  ← 问题位置     │           │  importToPostGIS()          │
│  line 53-73                │           │  line 178-231                │
└─────────────┬───────────────┘           └─────────────┬───────────────┘
              │                                         │
              │  SUPPORTED_FORMATS = ["shp","zip","geojson","json"]
              │
              │  switch (ext):
              │    "geojson"/"json" → parseGeoJSON()
              │    "shp"          → parseShapefileMetadata()
              │    "zip"          → ❌ 不处理，直接到 error
              │
              ▼
    GisDataParseResult.error("不支持的格式: zip")
```

## 结论

**根本原因**：`GisDataParserServiceImpl.parseFile()` 和 `importToPostGIS()` 方法虽然将 `zip` 列为支持格式，但代码中没有处理 `zip` 格式的逻辑分支。

**与 FormatDetector 的关系**：
- `FormatDetector` 的修复是正确的（返回 `UNKNOWN` 当 ZIP 内不含 Shapefile）
- 但 `GisDataParserServiceImpl` 完全绕过了 `FormatDetector`，自己独立做格式检查
- `GisDataParserServiceImpl` 依赖文件扩展名而非内容检测

## 修复建议

1. **短期**：在 `GisDataParserServiceImpl.parseFile()` 和 `importToPostGIS()` 中添加 `zip` 格式的处理分支，调用 `FormatDetector` 或直接复用 `VectorDataStoreFactory` 的逻辑

2. **长期**：统一格式检测逻辑，`GisDataParserServiceImpl` 应委托 `FormatDetector` 进行格式检测，而非自己维护一份 `SUPPORTED_FORMATS` 列表
