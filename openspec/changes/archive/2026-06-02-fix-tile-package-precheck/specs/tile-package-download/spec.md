# 瓦片包下载 (tile-package-download) — 变更 Delta

此 Delta 反映 `fix-tile-package-precheck` 变更对现有规格的修改。

## MODIFIED Requirements

### Requirement: 错误响应不使用 Servlet Writer 流

当下载过程中抛出异常时，系统 SHALL 不捕获异常，让异常自然传播到全局异常处理器 `GlobalExceptionHandler`，由其返回统一 JSON 错误响应。此方案可行的前提是：所有会导致异常的校验（包括空瓦片检查）都在 `response.getOutputStream()` 调用之前完成，因此异常抛出时 response 仍处于全新未使用状态。

#### Scenario: 空瓦片检查在输出流打开之前抛出异常
- **WHEN** `packageTiles()` 发现 GWC 目录无匹配瓦片文件
- **THEN** 异常在 `response.getOutputStream()` 调用之前抛出
- **AND** `GlobalExceptionHandler` 正常返回 `{ code: 500, message: "...", data: null }`

#### Scenario: 前置校验在输出流打开之前抛出异常
- **WHEN** `packageTiles()` 发现数据集不存在、未发布、目录不存在等
- **THEN** 异常在 `response.getOutputStream()` 调用之前抛出
- **AND** `GlobalExceptionHandler` 正常返回 `{ code: 500, message: "...", data: null }`

## UPDATED Interface List

以下触发异常的条件更新了错误描述以反映预检查机制：

**触发异常的校验**（更新项标 *）:
- 数据集不存在或已删除
- 数据集类型非 raster
- 数据集未发布
- 切片未完成（cacheSeedStatus != "seeded"）
- GeoServer data_dir 未配置
- 缩放级别超出范围
- 瓦片数量超出限制
- GWC 缓存目录不存在
- *没有匹配的瓦片文件（预检查发现无瓦片）
