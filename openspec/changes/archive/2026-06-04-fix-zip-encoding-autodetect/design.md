## Context

当前系统使用 `FormatDetector` 和 `MultiFormatImportService` 处理 Shapefile ZIP 文件导入。现有实现在 `ZipInputStream` 创建时硬编码使用 UTF-8 编码，这导致：

1. 中文文件名的 ZIP 包无法正确解压
2. 不同国家/地区导出的 Shapefile 可能使用不同编码（GBK、GB18030、ISO-8859-1 等）
3. `MalformedInputException` 异常导致导入失败

## Goals / Non-Goals

**Goals:**
1. 实现 ZIP 文件名编码自动检测，支持 UTF-8、GBK、GB18030、ISO-8859-1 等常见编码
2. 修复 FormatDetector 中的 ZIP 内容检测逻辑
3. 修复 MultiFormatImportService 中的 ZIP 解压逻辑
4. 统一编码处理方式，确保解析和解压使用一致的策略

**Non-Goals:**
- 不修改 GeoJSON、KML 等其他格式的处理逻辑
- 不添加新的矢量导入格式
- 不改变 API 接口契约

## Decisions

### 1. 编码检测策略

**选择方案：Fallback 机制**
- 依次尝试 UTF-8 → GBK → GB18030 → ISO-8859-1 → 系统默认编码
- 每种编码尝试解析 ZIP 条目名，成功则使用该编码

**备选方案：**
- 仅使用系统默认编码 - 无法处理大多数情况
- 要求用户指定编码 - 增加使用复杂度

### 2. ZIP 解压实现

**选择方案：ByteArrayOutputStream 缓存**
- 将整个 ZIP 文件读入字节数组
- 使用不同编码的 ZipInputStream 尝试解压
- 一旦成功，缓存该结果供后续使用

**备选方案：**
- 逐个条目尝试不同编码 - 性能较差
- 使用第三方库自动检测编码 - 增加依赖复杂度

### 3. 工具类设计

**选择方案：编码检测工具方法下沉到 FormatDetector**
- FormatDetector 添加静态方法 `detectCharset(byte[] data, String fallbackCharset)`
- MultiFormatImportService 调用 FormatDetector 的方法获取正确的编码
- 可复用于其他需要处理 ZIP 的场景

## Risks / Trade-offs

- **性能影响**: 多次尝试编码可能导致处理大文件时性能略有下降 → Mitigation：一旦检测成功即缓存结果，后续读取不再重复检测
- **编码误判**: 极端情况下可能选择错误编码 → Mitigation：使用 GB18030 作为最后的 fallback，它兼容 GBK 和 GB2312
