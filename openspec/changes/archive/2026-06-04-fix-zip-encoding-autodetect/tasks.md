## 1. FormatDetector 编码检测修复

- [x] 1.1 添加 detectCharset 方法，支持多种编码自动检测
- [x] 1.2 修改 detectZipContent 方法，使用编码自动检测替代硬编码 UTF-8
- [ ] 1.3 添加单元测试验证不同编码的 ZIP 文件检测

## 2. MultiFormatImportService 解压修复

- [x] 2.1 修改 extractShapefileFromZip 方法，使用编码检测逻辑
- [x] 2.2 添加基于 byte[] 缓存的解压实现，避免流重复打开
- [x] 2.3 验证解压后的文件名编码正确

## 3. 集成测试

- [ ] 3.1 使用 UTF-8 编码的 Shapefile ZIP 进行测试
- [ ] 3.2 使用 GBK 编码的 Shapefile ZIP 进行测试
- [ ] 3.3 使用 GB18030 编码的 Shapefile ZIP 进行测试
- [x] 3.4 编译验证通过
