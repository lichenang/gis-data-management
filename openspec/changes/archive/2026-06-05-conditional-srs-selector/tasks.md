## 1. 后端修改

- [x] 1.1 在 `GisDataParseResult.java` 中添加 `crsDetected` 字段
- [x] 1.2 修改 `MultiFormatImportServiceImpl.parseFile()` 方法，对 Shapefile 格式检测 CRS
- [x] 1.3 设置 `crsDetected` 和 `srs` 字段的值

## 2. 前端修改

- [x] 2.1 在 `api/dataset.ts` 的 `GisDataParseResult` 接口添加 `crsDetected` 字段
- [x] 2.2 在 `datasets/index.vue` 中添加 `isCrsDetected` 计算属性
- [x] 2.3 修改模板，条件显示"已自动识别"标签或源坐标系下拉框

## 3. 测试验证

- [x] 3.1 测试 Shapefile 带 .prj 文件时 parseFile 返回 `crsDetected: true`
- [x] 3.2 测试 Shapefile 不带 .prj 文件时 parseFile 返回 `crsDetected: false`
- [x] 3.3 验证前端根据 crsDetected 正确切换显示内容
