## 1. TilePackageServiceImpl — workspace fallback

- [x] 1.1 在 `TilePackageServiceImpl` 第 65 行 `geoServerProperties.getWorkspace()` 后添加 null/空检查，回退为 `"gisplatform"`
- [x] 1.2 验证：当 workspace 为 null/空时，GWC 目录路径正确拼接为 `{dataDir}/gwc/gisplatform_raster_{datasetId}`

## 2. GlobalExceptionHandler — response 已提交保护

- [x] 2.1 在 `handleRuntimeException` 方法的 `response.reset()` 前添加 `response.isCommitted()` 检查，若已提交则记录日志并返回 null
- [x] 2.2 在 `handleException` 方法同样添加 `response.isCommitted()` 检查
- [x] 2.3 验证：Controller 提前调用 `getOutputStream()` 后 Service 抛出异常时，GlobalExceptionHandler 不抛 IllegalStateException，日志正确记录

## 3. 构建验证

- [x] 3.1 Maven 编译无错误
- [x] 3.2 测试切片包下载接口，确认异常场景返回有效 JSON 错误信息
