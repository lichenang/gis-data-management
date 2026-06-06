## 1. FormatDetector 修复

- [x] 1.1 审查 detect() 方法：当文件扩展名为 .zip 时，增加对 ZIP 包内容的深度检测
- [x] 1.2 修复 detectZipContent() 方法：确保异常时返回 UNKNOWN 而不是 SHAPEFILE

## 2. 验证

- [x] 2.1 执行 `mvn compile` 验证编译通过
- [x] 2.2 单元测试 FormatDetector 验证 ZIP 内含 Shapefile 的检测逻辑
