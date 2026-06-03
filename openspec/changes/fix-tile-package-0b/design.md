## Context

当前下载瓦片包在特定条件下返回 0B 空文件。经诊断定位到三个 Bug：

1. **Controller (`TilePackageController.java`)**: Catch 块中调用 `response.getWriter()`，但 try 块已调用 `response.getOutputStream()`。Servlet API 禁止混用，抛出 `IllegalStateException` 被静默吞噬，响应以 `Content-Type: application/zip` + 0 字节 body 返回
2. **parseBounds (`TilePackageServiceImpl.java`)**: 直接从 `dataset.extent` 读取 minX/minY/maxX/maxY 用于瓦片坐标计算，但 extent 存储在源 CRS（如 EPSG:32650 UTM 米制），而瓦片公式假设 EPSG:4326 度值。两者不匹配时，计算出的瓦片坐标全部越界，找不到任何文件
3. **tileY 公式 (`TilePackageServiceImpl.java`)**: 硬编码 Web Mercator 公式 `(1 - log(tan(lat) + 1/cos(lat)) / π) / 2 * 2^z`。GWC 种子任务通过 `bounds=-180,-90,180,90` (EPSG:4326) 触发，若默认网格集为 EPSG:4326，瓦片 Y 应为线性公式 `(90 - lat) / 180 * 2^z`

三个 Bug 共同作用：CRS 未转换 + 公式不匹配 → 瓦片范围全越界 → totalWritten=0 → 抛出异常 → getOutputStream/getWriter 冲突 → 0B 响应。

## Goals / Non-Goals

**Goals:**
- 修复 Controller 异常处理路径，确保错误时返回正确 JSON 响应
- 修复 parseBounds 中 extent 的 CRS 转换，使其始终在 EPSG:4326 下计算
- 修复 tileY 公式，使其与 GWC 默认网格集匹配

**Non-Goals:**
- 不改动 GWC 种子任务（seeding）的实现方式
- 不添加新的 API 或修改请求/响应结构
- 不涉及前端下载交互逻辑
- 不实现运行时 GWC 网格集自动检测（可通过后续改进添加）

## Decisions

### 决策 1：Controller 异常处理改为 throw，由全局处理器接管

**方案**：移除 catch 块中 `response.getWriter()` 调用，直接抛出 `RuntimeException`（或原异常），由已在项目中注册的 `GlobalExceptionHandler`（使用 `@RestControllerAdvice`）统一处理。

**替代方案**：在 catch 块中通过 `response.getOutputStream()` 重新写 JSON。该方案也可行，但需要在 catch 中重置响应头并处理 `getOutputStream()` 再次调用的边界情况。让全局处理器统一处理更符合项目架构。

### 决策 2：parseBounds 增加 CRS 转换

**方案**：从 `dataset.extent` 解析出原始值后，获取 `dataset.getSrs()`（如 `"EPSG:32650"`），调用 `CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs)` 转换到 EPSG:4326。转换失败时回退到全局范围 `(-180, -90, 180, 90)`。

**注意**：`CrsTransformUtil.transformExtentToWgs84()` 在 sourceCrs 为 null/空 或等于 EPSG:4326 时返回 null。需在 parseBounds 中处理这两种情况：
- sourceCrs 为 null/空 → 直接使用原始 extent 值（假设已是 4326）并加 WARN 日志
- sourceCrs 为 EPSG:4326 → 直接使用原始 extent 值

### 决策 3：tileY 改为 EPSG:4326 线性公式

**方案**：将 `tileY()` 方法中的 Web Mercator 公式替换为 EPSG:4326 网格集（GlobalCRS84Geographic）的线性公式：

```
tileY(lat, z) = (90 - lat) / 180 * 2^z
```

**理由**：
- GWC 种子 API 调用以 EPSG:4326 度值发送 bounds，未指定 gridSetId
- 对于 bounds 以 EPSG:4326 提交的种子任务，GWC 的默认网格集行为倾向于使用 EPSG:4326 (GlobalCRS84Geographic)
- EPSG:4326 网格集的 Y 坐标是纬度的线性映射，而 Web Mercator 是非线性投影，即使正确 extent 代入 Web Mercator 公式也会得到不同的行号
- X 公式 `(lon + 180) / 360 * 2^z` 在两种网格集中相同，不需修改

**风险**：若实际 GWC 配置使用 `EPSG:900913` (Web Mercator) 网格集，线性 Y 公式会计算出错误的行号。这种情况下需要反向修改为 Web Mercator 公式。由于无法从 Java 运行时探测 GWC 网格集配置，选择 EPSG:4326 作为默认值，后续可通过检查 GWC layer config XML 确认。

## Risks / Trade-offs

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| EPSG:4326 网格集假设错误（实际为 Web Mercator） | 瓦片 Y 坐标偏差，0B 可复现 | 若验证发现该情况，将 tileY 改回 Web Mercator 并标记 `dataset.srs` 为修改依据 |
| CrsTransformUtil.transformExtentToWgs84 可能失败（GeoTools CRS 解码异常） | extent 回退到全幅范围，计算瓦片范围过大 | WARN 日志记录转换失败详情；大范围仍能找到实际存在的瓦片（总比 0B 好） |
| GlobalExceptionHandler 未处理输出流写入中的异常 | 全局处理也可能失败 | 全局处理器已有 `try-catch` 兜底，写入错误时返回空响应最坏情况与当前一致 |
