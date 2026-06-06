## Context

`VectorDataStoreFactory.extractZipToTempDir()` 方法在解压 ZIP 文件时存在编码问题：

```java
// 当前实现 - 第 131 行
try (ZipInputStream zis = new ZipInputStream(inputStream)) {
```

`ZipInputStream(InputStream)` 构造函数默认使用 UTF-8 编码解析文件名。当 ZIP 包使用 GBK 或 GB18030 编码时，中文文件名会产生乱码或解压失败。

## Goals / Non-Goals

**Goals:**
- 修复 `extractZipToTempDir()` 的编码问题
- 实现编码自动检测（UTF-8, GBK, GB18030）
- 复用 `FormatDetector.detectZipCharset()` 的现有检测逻辑

**Non-Goals:**
- 不修改 FormatDetector 的现有实现
- 不支持其他罕见编码

## Decisions

### Decision 1: 复用 FormatDetector 的编码检测

**方案**：在 `VectorDataStoreFactory` 中调用 `FormatDetector.detectZipCharset()` 进行编码检测

**理由**：
- `FormatDetector.detectZipCharset(byte[])` 已实现完整的编码检测逻辑
- 避免代码重复
- 保持一致性

**替代方案**（被否决）：
- 在 `VectorDataStoreFactory` 中复制 `FormatDetector.detectZipCharset()` 的代码 → 代码重复
- 直接使用 GBK 硬编码 → 不支持 UTF-8 编码的文件

### Decision 2: 修改 `extractZipToTempDir()` 签名

**方案**：将 `extractZipToTempDir(InputStream, File)` 改为 `extractZipToTempDir(byte[], File)`，以支持编码检测

**理由**：
- `FormatDetector.detectZipCharset()` 需要 `byte[]` 作为输入
- 修改签名使编码检测逻辑更清晰

## 实现方案

修改 `VectorDataStoreFactory.java`：

1. 将 `extractZipToTempDir` 方法的输入参数从 `InputStream` 改为 `byte[]`
2. 在方法内部调用 `FormatDetector.detectZipCharset()` 检测编码
3. 使用检测到的编码创建 `ZipInputStream`

```java
private File extractZipToTempDir(byte[] zipData, File tempDir) throws IOException {
    Charset charset = FormatDetector.detectZipCharset(zipData);
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData), charset)) {
        // ... 解压逻辑
    }
}
```

4. 修改调用方 `createShapefileDataStore()` 和 `getInputStream()`，将 `InputStream` 转换为 `byte[]`

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 检测逻辑在解压大文件时性能略有下降 | `detectZipCharset()` 只读取 ZIP 头部，性能影响可忽略 |
| 编码检测失败时默认 UTF-8 | 与当前行为一致，不引入新问题 |
