## 1. 修复 tileY 公式（使用 Web Mercator）

- [ ] 1.1 将 `TilePackageServiceImpl.tileY()` 从 EPSG:4326 线性公式 `(90 - lat) / 180 * (1 << z)` 改回 Web Mercator 公式
- [ ] 1.2 验证 getTileRange() 中的 X 公式 `(lon + 180) / 360 * (1 << z)` 无需修改（EPSG:900913 和 EPSG:4326 的 X 公式相同）

## 2. 修复 GlobalExceptionHandler Content-Type 冲突

- [ ] 2.1 在 `handleRuntimeException()` 和 `handleException()` 方法签名中添加 `HttpServletResponse response` 参数
- [ ] 2.2 在两个方法开头添加 `response.reset()` 和 `setContentType("application/json;charset=UTF-8")`
- [ ] 2.3 将 reset/setContentType 调用包装在 try-catch 中，防止 response 已部分提交时失败

## 3. 编译验证

- [ ] 3.1 运行 `mvn compile -f backend/pom.xml` 确认所有修改编译通过
- [ ] 3.2 检查无新增编译警告
