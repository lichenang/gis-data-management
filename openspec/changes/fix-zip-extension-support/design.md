## Context

系统通过 ZIP 包上传 Shapefile 时，`VectorFileFormat` 枚举已包含 `"zip"` 为 Shapefile 的有效扩展名。但 `GisDataParserServiceImpl` 中的 `SUPPORTED_FORMATS` 数组不包含 `"zip"`，导致上传被拒绝。

上一个变更 `fix-shapefile-zip-format` 已完成：在 `SUPPORTED_FORMATS` 中添加了 `"zip"`。

## Goals / Non-Goals

**Goals:**
- 确认修复已生效，上传 .zip 文件不再被拒绝

**Non-Goals:**
- 不修改其他代码

## Risks / Trade-offs

无风险。
