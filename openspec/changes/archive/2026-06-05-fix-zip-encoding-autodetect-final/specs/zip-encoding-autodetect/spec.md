# zip-encoding-autodetect 规格说明

## 模块划分

- **解压层**: VectorDataStoreFactory.extractZipToTempDir() - 处理 ZIP 文件名编码

## ADDED Requirements

### Requirement: ZIP 文件名编码自适应

VectorDataStoreFactory SHALL 在解压 ZIP 文件时自动检测并使用正确的字符编码。

#### Scenario: UTF-8 编码的 ZIP 文件
- **WHEN** 解压使用 UTF-8 编码的 ZIP 文件（含中文文件名）
- **THEN** 文件名正确解压，无乱码

#### Scenario: GBK 编码的 ZIP 文件
- **WHEN** 解压使用 GBK 编码的 ZIP 文件（含中文文件名）
- **THEN** 文件名正确解压，无乱码

#### Scenario: GB18030 编码的 ZIP 文件
- **WHEN** 解压使用 GB18030 编码的 ZIP 文件（含中文文件名）
- **THEN** 文件名正确解压，无乱码

#### Scenario: 编码检测失败
- **WHEN** 无法确定 ZIP 文件编码
- **THEN** 使用 UTF-8 作为默认编码

## 数据流设计

```
extractZipToTempDir(byte[] zipData, File tempDir)
    │
    ▼
FormatDetector.detectZipCharset(zipData)
    │
    ├── 尝试 UTF-8, GBK, GB18030, ISO-8859-1, US-ASCII
    └── 返回检测到的 Charset（或默认 UTF-8）
    │
    ▼
new ZipInputStream(ByteArrayInputStream(zipData), charset)
    │
    ▼
遍历 ZIP 条目，按检测到的编码解析文件名
```

## 接口列表

| 方法 | 入参 | 返回值 | 说明 |
|------|------|--------|------|
| `extractZipToTempDir(byte[] zipData, File tempDir)` | ZIP 字节数据, 目标目录 | `File` | 解压 ZIP 并返回目标目录 |
