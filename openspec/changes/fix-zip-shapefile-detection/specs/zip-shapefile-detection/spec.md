# zip-shapefile-detection 规格说明

## 模块划分

- **检测层**: FormatDetector.formatDetection() - 处理 ZIP 文件格式检测

## ADDED Requirements

### Requirement: ZIP 包内 Shapefile 检测

FormatDetector SHALL 对扩展名为 .zip 的文件进行深度检测，识别其内部是否包含 .shp 文件。

#### Scenario: ZIP 包包含 Shapefile
- **WHEN** 调用 `detect(file, "data.zip")` 且 ZIP 包内存在 .shp 文件
- **THEN** 返回 `VectorFileFormat.SHAPEFILE`

#### Scenario: ZIP 包不包含 Shapefile
- **WHEN** 调用 `detect(file, "data.zip")` 且 ZIP 包内不存在 .shp 文件
- **THEN** 返回 `VectorFileFormat.UNKNOWN`

#### Scenario: 普通 ZIP 文件（非 Shapefile）
- **WHEN** 调用 `detect(file, "data.zip")` 且 ZIP 包内不存在任何支持的矢量格式
- **THEN** 返回 `VectorFileFormat.UNKNOWN`

## 数据流设计

```
detect(file, filename)
├── extension = getExtension(filename)
├── format = fromExtension(extension)
├── if format == UNKNOWN → return UNKNOWN
├── if format == SHAPEFILE && filename.endsWith(".zip")
│   └── detectZipContent(file)
│       ├── detectZipCharset(zipData)
│       ├── scanZipForFormat(zis)
│       │   └── 遍历 ZIP 条目，查找 .shp / .kml 扩展名
│       └── 返回检测到的格式或 SHAPEFILE（默认）
└── return format
```

## 接口列表

| 方法 | 入参 | 返回值 | 说明 |
|------|------|--------|------|
| `detect(MultipartFile file, String filename)` | 文件对象, 文件名 | `VectorFileFormat` | 自动识别文件格式 |
