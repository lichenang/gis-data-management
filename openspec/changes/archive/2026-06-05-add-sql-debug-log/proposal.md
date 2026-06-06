## Why

当前 `insertFeature` 方法出现 `"栏位索引超过许可范围：10，栏位数：9"` 错误，需要添加调试日志来定位 SQL 占位符数量与参数数量不匹配的根本原因。

## What Changes

- 在 `MultiFormatImportServiceImpl.insertFeature()` 方法中添加 SQL 生成调试日志
- 添加占位符数量与列名数量的验证逻辑
- 在异常发生前抛出包含详细信息的诊断异常

## Capabilities

### Modified Capabilities

- `multi-format-vector-import`: 添加调试日志和验证逻辑

## Impact

### 受影响的文件

- `backend/src/main/java/com/gisplatform/service/impl/MultiFormatImportServiceImpl.java`

### 非目标

- 不修改 SQL 构建逻辑（仅添加日志）
- 不修改业务逻辑
