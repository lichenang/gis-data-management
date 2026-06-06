## 1. 后端修复

- [x] 1.1 修改 `MultiFormatImportServiceImpl.importToPostGIS()` 方法，添加 `sourceSrs` 参数并传递给 `importUsingDataStore()`
- [x] 1.2 修改 `MultiFormatImportServiceImpl.importUsingDataStore()` 方法签名，添加 `sourceSrs` 参数
- [x] 1.3 实现 fallback 逻辑：当 `nativeSrid == 0` 且 `sourceSrs` 有效时，使用 `sourceSrs` 作为源坐标系
- [x] 1.4 添加必要的日志记录，记录使用用户指定 sourceSrs 的情况

## 2. 前端提示

- [x] 2.1 在上传对话框的坐标系选择区域，检查 Shapefile 是否缺少 .prj 文件
- [x] 2.2 如果缺少 .prj 文件，显示提示文字："请手动选择原始投影"
- [x] 2.3 添加源坐标系选择器（当检测到缺少 .prj 文件时显示）

## 3. 测试与验证

- [x] 3.1 使用缺少 .prj 文件的 Shapefile 测试，指定 sourceSrs，验证正确转换
- [x] 3.2 验证日志输出包含正确的 sourceSrs 信息
