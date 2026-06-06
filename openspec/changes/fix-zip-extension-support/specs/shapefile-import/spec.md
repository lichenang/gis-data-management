# Shapefile ZIP 格式支持规格说明

## 模块划分

- **数据解析层** (`GisDataParserServiceImpl`): 检查 SUPPORTED_FORMATS 并解析文件

## 数据流设计

```
用户上传 (.zip)
        │
        ▼
 getFileExtension() → "zip"
        │
        ▼
 isSupportedFormat("zip") → true (修复后)
```

## 接口列表

| 接口 | 说明 |
|------|------|
| `parseFile(MultipartFile, String)` | 解析文件元数据 |

## ADDED Requirements

### Requirement: 支持 .zip 扩展名导入 Shapefile

系统 SHALL 允许用户上传 `.zip` 文件（包含 Shapefile 文件）进行数据导入。

#### Scenario: 上传 ZIP 包不再被拒绝
- **WHEN** 用户上传 `.zip` 文件（包含 .shp/.shx/.dbf/.prj）
- **THEN** 系统不返回"不支持的格式"错误，而是继续处理
