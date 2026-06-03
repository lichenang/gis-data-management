## 1. 修复 Controller 异常处理（Bug #1）

- [x] 1.1 移除 `TilePackageController.downloadTilePackage()` catch 块中 `response.getWriter()` 调用，改用 `response.reset()` + `getOutputStream()` 写 JSON 错误
- [x] 1.2 确认 `writeErrorResponse()` 方法通过 `response.reset()` 清空流状态后正确输出 JSON 错误（已验证 `reset()` 会清除 ServletOutputStream 的 usingOutputStream 标志）

## 2. 修复 parseBounds CRS 转换（Bug #2）

- [x] 2.1 在 `parseBounds()` 中从 `dataset.extent` 解析 minX/minY/maxX/maxY 后，读取 `dataset.getSrs()` 获取源 CRS
- [x] 2.2 调用 `CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs)` 将 extent 转换到 EPSG:4326
- [x] 2.3 处理边界情况：sourceCrs 为 null/空时直接使用原始值并输出 WARN 日志；转换失败时回退到原始 extent 值（非全幅范围，减少过度扫描）

## 3. 修复 tileY 坐标计算公式（Bug #3）

- [x] 3.1 将 `tileY()` 方法中的 Web Mercator 公式替换为 EPSG:4326 线性公式：`(90 - lat) / 180 * (1 << z)`
- [x] 3.2 确认 `getTileRange()` 中的 X 坐标公式保持不变（`(lon + 180) / 360 * (1 << z)` 在两种网格集中一致）
- [x] 3.3 `tileY()` 仅被 `getTileRange()` 引用，`estimateTileCount()` 复用 `getTileRange()`，无需额外修改

## 4. 编译验证

- [x] 4.1 运行 `mvn compile -f backend/pom.xml` 确认所有修改编译通过
- [x] 4.2 检查无新增编译警告（仅存在 `SecurityConfig.java` 的 `deprecation` 和 `ImageServiceImpl.java` 的 `unchecked` 预存警告）
