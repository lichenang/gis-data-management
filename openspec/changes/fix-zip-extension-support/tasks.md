## 1. 验证修复

- [x] 1.1 确认 `GisDataParserServiceImpl.java` 的 `SUPPORTED_FORMATS` 包含 `"zip"`

## 2. 测试

- [ ] 2.1 手动测试上传 .zip 文件验证不再被拒绝

## 完成

修复已在 `fix-shapefile-zip-format` 变更中完成：
- 文件: `GisDataParserServiceImpl.java`
- 修改: `SUPPORTED_FORMATS = {"shp", "zip", "geojson", "json"}`
