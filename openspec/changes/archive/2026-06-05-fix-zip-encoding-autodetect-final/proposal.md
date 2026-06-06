## Why

当前 `VectorDataStoreFactory.extractZipToTempDir()` 方法在解压 ZIP 文件时使用硬编码的 UTF-8 编码。当 Shapefile ZIP 包包含中文文件名时，解压会失败或产生乱码。用户上传不同编码环境（如 Windows GBK、Chinese Simplified GB18030）创建的 ZIP 包时，无法正确处理。

## What Changes

- 修改 `VectorDataStoreFactory.extractZipToTempDir()` 方法，增加编码自动检测逻辑
- 复用 `FormatDetector.detectZipCharset()` 的编码检测实现
- 支持 UTF-8、GBK、GB18030 三种常见编码的自动检测

## Capabilities

### New Capabilities
- `zip-encoding-autodetect`: ZIP 文件名编码自适应检测

### Modified Capabilities
<!-- 空：现有 FormatDetector 的 ZIP 编码检测能力被复用 -->

## Impact

**受影响文件**:
- `backend/src/main/java/com/gisplatform/service/VectorDataStoreFactory.java`

## Non-goals

- 不修改 FormatDetector 的现有实现
- 不支持其他罕见编码（如 Big5、Shift-JIS）
- 不修改 ZIP 内容解析逻辑，仅处理文件名编码
