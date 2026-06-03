## Why

当 `geoserver.data-dir` 配置为空或指向不存在的目录时，`TilePackageServiceImpl` 在打开 `ZipOutputStream` 之前抛出 `RuntimeException`，该异常被 `GlobalExceptionHandler` 捕获后返回 JSON 错误。但现有错误提示仅说明"GeoServer data_dir 未配置"，未区分"未配置"与"目录不存在"两种情况；且默认路径 `D:\Program Files\geoserver-2.28.3-bin\data_dir` 在 Linux/macOS 开发环境中不存在，导致功能完全不可用。用户需要能够明确知道配置问题所在，以及在配置缺失时系统如何降级。

## What Changes

1. **application.yml**：将 `data-dir` 默认值改为空字符串，移除特定于 Windows 的路径，避免在 Linux/macOS 上产生误导性的默认配置
2. **GeoServerProperties.java**：`dataDir` 字段增加 `@Builder.Default` 注解，确保空值时不影响默认值语义
3. **TilePackageServiceImpl**：改进 `data_dir` 校验逻辑，区分"未配置"与"目录不存在"两种错误情况，分别抛出不同消息；同时在 `data_dir` 为空时，提前检测并抛出异常，避免后续空指针风险
4. **错误处理一致性**：所有校验异常均在 `ZipOutputStream` 创建之前抛出，确保 `GlobalExceptionHandler` 能正确返回 JSON 错误

## Capabilities

### New Capabilities
- `geoserver-config`：GeoServer 配置的健壮性改进，包括 data_dir 的空值/无效路径检测与明确错误提示

### Modified Capabilities
（无 — 当前变更为内部实现改进，不改变既有 API 规格）

## Non-goals

- 不修改 GlobalExceptionHandler
- 不改变 API 请求/响应格式
- 不添加新的 API 端点
- 不修改 GeoServer REST API 调用逻辑

## Impact

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `backend/src/main/resources/application.yml` | 修改 | data-dir 默认值改为空字符串 |
| `backend/src/main/java/com/gisplatform/config/GeoServerProperties.java` | 修改 | dataDir 字段增加 @Builder.Default 注解 |
| `backend/src/main/java/com/gisplatform/service/impl/TilePackageServiceImpl.java` | 修改 | 改进 data_dir 校验逻辑，区分未配置与目录不存在 |
