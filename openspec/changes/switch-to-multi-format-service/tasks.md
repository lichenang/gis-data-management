## 1. 创建 MultiFormatImportService

- [x] 1.1 创建 `MultiFormatImportService` 接口，定义 `parseFile()`、`importToPostGIS()`、`getSupportedFormats()` 方法
- [x] 1.2 创建 `MultiFormatImportServiceImpl` 实现类，组合 `FormatDetector` 和 `VectorDataStoreFactory`

## 2. 修改 DatasetServiceImpl

- [x] 2.1 在 `DatasetServiceImpl` 中注入 `MultiFormatImportService`
- [x] 2.2 修改 `parseUploadFile()` 方法，改用 `MultiFormatImportService.parseFile()`
- [x] 2.3 修改 `importDataset()` 方法，改用 `MultiFormatImportService.importToPostGIS()`

## 3. 验证

- [x] 3.1 执行 `mvn compile` 验证编译通过
- [ ] 3.2 启动应用并测试 `/api/v1/datasets/parse` 接口（上传 ZIP 包）
