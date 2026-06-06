## 1. 修复 SUPPORTED_FORMATS

- [x] 1.1 在 `GisDataParserServiceImpl.java` 的 `SUPPORTED_FORMATS` 数组中添加 `"zip"`

## 2. 验证

- [x] 2.1 执行 `mvn compile` 验证编译通过
- [ ] 2.2 测试上传 .zip 文件（包含 shapefile）验证不再返回"不支持的格式"错误

## 完成

- 修改文件: `backend/src/main/java/com/gisplatform/service/impl/GisDataParserServiceImpl.java`
- 修改内容: `SUPPORTED_FORMATS` 数组添加 `"zip"`
