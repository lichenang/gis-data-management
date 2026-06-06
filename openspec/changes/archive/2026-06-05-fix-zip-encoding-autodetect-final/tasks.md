## 1. 修改 VectorDataStoreFactory

- [x] 1.1 修改 `extractZipToTempDir()` 方法签名：输入参数从 `InputStream` 改为 `byte[]`
- [x] 1.2 在 `extractZipToTempDir()` 内调用 `FormatDetector.detectZipCharset()` 检测编码
- [x] 1.3 使用检测到的编码创建 `ZipInputStream`

## 2. 更新调用方

- [x] 2.1 修改 `createShapefileDataStore()` 中调用 `extractZipToTempDir()` 的代码
- [x] 2.2 修改 `getInputStream()` 中调用 `extractZipToTempDir()` 的代码

## 3. 验证

- [x] 3.1 执行 `mvn compile` 验证编译通过
