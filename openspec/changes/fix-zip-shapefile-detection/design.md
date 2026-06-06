## Context

`FormatDetector.detect()` 方法当前在检测 ZIP 文件时存在问题：
- 当文件扩展名为 .zip 时，`VectorFileFormat.fromExtension("zip")` 返回 SHAPEFILE（因为 zip 是 SHAPEFILE 的合法扩展名之一）
- 但 `detectZipContent()` 在检测失败时默认返回 SHAPEFILE，导致即使 ZIP 内不含 Shapefile 也会被识别为 Shapefile
- 用户上传不包含 Shapefile 的 ZIP 包时被拒绝

## Goals / Non-Goals

**Goals:**
- 修复 FormatDetector 对 ZIP 包内 Shapefile 的检测逻辑
- 确保当 ZIP 包内不存在 .shp 文件时返回 UNKNOWN

**Non-Goals:**
- 不修改 VectorFileFormat 枚举结构
- 不添加新的格式支持

## Decisions

### Decision 1: 修复 detectZipContent 默认返回值

`detectZipContent()` 方法在检测失败时默认返回 SHAPEFILE，这会导致误判。

**方案**：当 ZIP 内容检测成功但未找到有效格式时返回 `null`，然后在调用处处理 null 情况返回 UNKNOWN。

### Decision 2: 增加 ZIP 内容检测的鲁棒性

当前 `scanZipForFormat()` 在检测到 .shp 文件时直接返回 SHAPEFILE，这是正确的。

**保持不变**：scanZipForFormat 的核心逻辑无需修改。

## Risks / Trade-offs

无显著风险。修改范围仅限于 FormatDetector.java 的错误处理逻辑。
