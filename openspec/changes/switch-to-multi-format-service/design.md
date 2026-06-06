## Context

当前 `DatasetServiceImpl` 使用 `GisDataParserService` 处理数据集解析和导入。但 `GisDataParserServiceImpl` 存在以下问题：

1. **格式检测逻辑重复**：`GisDataParserServiceImpl` 自己维护一份 `SUPPORTED_FORMATS` 列表，而不是使用 `FormatDetector` 进行深度检测
2. **不支持 ZIP 内 Shapefile**：`parseFile()` 和 `importToPostGIS()` 方法虽然接受 `zip` 扩展名，但没有实际处理逻辑
3. **导入功能不完整**：仅支持 GeoJSON 格式导入

## Goals / Non-Goals

**Goals:**
- 创建统一的 `MultiFormatImportService` 服务
- 集成 `FormatDetector` 进行格式深度检测
- 支持 Shapefile (包括 ZIP 包)、GeoJSON、KML、GML、GPX、CSV 等格式
- 修改 `DatasetServiceImpl` 使用新服务

**Non-Goals:**
- 不删除 `GisDataParserService` 接口和实现
- 不修改 `VectorDataStoreFactory` 的实现
- 不实现数据导出功能

## Decisions

### Decision 1: 创建新服务而非修改旧服务

**方案**：创建新的 `MultiFormatImportService`，而非修改 `GisDataParserServiceImpl`

**理由**：
- 保持向后兼容，避免影响其他依赖 `GisDataParserService` 的代码
- 新服务可以采用更清晰的设计，不受历史包袱约束
- 逐步迁移而非一次性替换，降低风险

### Decision 2: 委托 FormatDetector 进行格式检测

新服务将组合使用 `FormatDetector` 和 `VectorDataStoreFactory`：

```
MultiFormatImportService
    │
    ├── FormatDetector.detect()  → 深度检测 ZIP 包内容
    │
    └── VectorDataStoreFactory.createDataStore()  → 创建对应格式的 DataStore
```

### Decision 3: 统一返回类型

新服务复用现有的 `GisDataParseResult` 和 `DatasetImportResult` DTO，保持与现有接口的兼容性。

## 新服务接口设计

```java
public interface MultiFormatImportService {

    /**
     * 解析空间数据文件并返回元数据
     */
    GisDataParseResult parseFile(MultipartFile file, String fileName);

    /**
     * 导入空间数据到PostGIS
     */
    DatasetImportResult importToPostGIS(MultipartFile file, String fileName,
                                         String datasetName, String targetSrs);

    /**
     * 获取支持的文件格式列表
     */
    String[] getSupportedFormats();
}
```

## 数据流

```
parseFile(file, "data.zip")
    │
    ▼
FormatDetector.detect(file, "data.zip")
    │
    ├── format = fromExtension("zip") → SHAPEFILE
    ├── 检测到 .zip 扩展名
    └── scanZipForFormat() → 遍历 ZIP 条目查找 .shp
          │
          ▼
    返回 VectorFileFormat.SHAPEFILE
    │
    ▼
VectorDataStoreFactory.createDataStore(SHAPEFILE, file, "data.zip")
    │
    ├── 检测到 .zip，提取到临时目录
    ├── 查找 .shp 文件
    └── 创建 ShapefileDataStore
```

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 新服务与旧服务行为不一致 | 逐步迁移，先切换 parseFile，后续再迁移 importDataset |
| FormatDetector 检测失败 | 返回 UNKNOWN，调用方处理 |
| Zip 编码检测不准确 | 使用多种编码尝试检测 |

## Migration Plan

1. **Phase 1**: 创建 `MultiFormatImportService` 接口和实现
2. **Phase 2**: 修改 `DatasetServiceImpl` 的 `parseUploadFile()` 使用新服务
3. **Phase 3**: 修改 `DatasetServiceImpl` 的 `importDataset()` 使用新服务
4. **Phase 4**: 验证所有格式的解析和导入功能
