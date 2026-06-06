# Shapefile ZIP 格式支持规格说明

## 模块划分

- **数据解析层** (`GisDataParserServiceImpl`): 检查 SUPPORTED_FORMATS 并解析文件

## 数据流设计

```
用户上传 (.zip)
        │
        ▼
 GisDataParserServiceImpl.parseFile()
        │
        ▼
 getFileExtension() → "zip"
        │
        ▼
 isSupportedFormat("zip") → false × [问题点]
        │
        ▼
 返回错误: 不支持的格式
```

修复后流程：
```
用户上传 (.zip)
        │
        ▼
 GisDataParserServiceImpl.parseFile()
        │
        ▼
 getFileExtension() → "zip"
        │
        ▼
 isSupportedFormat("zip") → true ✓
        │
        ▼
 FormatDetector.detect() → SHAPEFILE (ZIP包含.shp)
```

## 接口列表

| 接口 | 说明 |
|------|------|
| `parseFile(MultipartFile, String)` | 解析文件元数据 |

## MODIFIED Requirements

以下内容是对 `multi-format-vector-support.md` 中相关规范的修改：

**原始描述：**
> 导入格式: Shapefile | `.shp` | `ShapefileDataStore` | 需同时上传 .shx, .dbf, .prj 等

**更新后：**
> 导入格式: Shapefile | `.shp`, `.zip` | `ShapefileDataStore` | 支持 ZIP 打包上传

### Requirement: 支持通过 ZIP 包上传 Shapefile

系统 SHALL 允许用户上传包含 Shapefile 文件（.shp/.shx/.dbf/.prj 等）的 ZIP 包进行数据导入。

#### Scenario: 成功上传 ZIP 包
- **WHEN** 用户上传 `.zip` 文件（包含 .shp/.shx/.dbf/.prj）
- **THEN** 系统正确识别格式为 Shapefile，不返回"不支持的格式"错误
