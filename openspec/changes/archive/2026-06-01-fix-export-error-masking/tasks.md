## 1. SecurityConfig 兜底

- [x] 1.1 在 `SecurityConfig.java` 的 `permitAll` 列表中添加 `/error` 路径

## 2. ExportController 错误处理改造

- [x] 2.1 替换 `exportDataset()` 中数据集不存在的 `setStatus(404)` + `return` 为 JSON 错误响应写入
- [x] 2.2 替换 `exportDataset()` catch 块中的 `sendError(500, ...)` 为 JSON 错误响应写入
- [x] 2.3 替换 `exportDataset()` 方法内 `sendError(400, ...)` 调用（格式不支持、类型不匹配、未知类型）为 JSON 错误响应写入
- [x] 2.4 删除 `exportDataset()` catch 块中嵌空的 try-catch

## 3. 编译验证

- [x] 3.1 运行 `mvn compile` 确认编译通过
